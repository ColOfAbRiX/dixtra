import * as vscode from "vscode";
import * as path from "node:path";
import * as fs from "node:fs/promises";
import * as os from "node:os";
import { spawn } from "node:child_process";

const LANGUAGES = ["scala"];

let extensionPath = "";
let channel: vscode.LogOutputChannel;
let binaryVersion: string | null = null;

export function activate(context: vscode.ExtensionContext): void {
  extensionPath = context.extensionPath;
  channel = vscode.window.createOutputChannel("dixtra", { log: true });
  context.subscriptions.push(channel);

  const version = (context.extension.packageJSON as { version?: string }).version ?? "unknown";

  channel.info(`dixtra ${version} starting on VS Code ${vscode.version}`);
  channel.info(`platform ${process.platform}-${process.arch}, extension at ${extensionPath}`);
  channel.debug(
    `node ${process.version}, ${os.type()} ${os.release()}, locale ${process.env.LANG ?? "unknown"}, ` +
      `workspace ${vscode.workspace.workspaceFolders?.map(folder => folder.uri.fsPath).join(", ") || "none"}`,
  );

  logConfiguration();

  context.subscriptions.push(
    vscode.workspace.onDidChangeConfiguration(event => {
      if (event.affectsConfiguration("dixtra")) {
        logConfiguration();
      }
    }),
  );

  const provider = new DixtraFormatter();
  context.subscriptions.push(
    vscode.languages.registerDocumentFormattingEditProvider(LANGUAGES, provider),
  );

  channel.debug(`registered a formatting provider for ${LANGUAGES.join(", ")}`);
}

type FormatResult =
  | { kind: "ok" }
  | { kind: "failed"; message: string };

class DixtraFormatter implements vscode.DocumentFormattingEditProvider {
  async provideDocumentFormattingEdits(
    document: vscode.TextDocument,
  ): Promise<vscode.TextEdit[]> {
    channel.debug(
      `format requested on ${document.uri.toString()}, scheme ${document.uri.scheme}, ` +
        `language ${document.languageId}, dirty ${document.isDirty}, ${document.getText().length} chars`,
    );

    if (document.uri.scheme !== "file") {
      channel.info(`not formatting ${document.uri.toString()}: it is not a file on disk`);
      void vscode.window.showInformationMessage("dixtra formats files on disk: save this document first.");
      return [];
    }

    if (document.isDirty && !(await document.save())) {
      channel.error(`could not save ${document.uri.fsPath}`);
      void vscode.window.showErrorMessage("dixtra: the document could not be saved.");
      return [];
    }

    const result = await formatFile(document.uri.fsPath);
    if (result.kind === "failed") {
      channel.error(`formatting ${document.uri.fsPath} failed: ${result.message}`);
      void vscode.window.showErrorMessage(`dixtra: ${result.message}`);
      return [];
    }

    const before = document.getText();
    const after = await fs.readFile(document.uri.fsPath, "utf8");
    if (after === before) {
      channel.info(`no edits for ${document.uri.fsPath}`);
      return [];
    }

    channel.info(`edits produced for ${document.uri.fsPath}`);

    const whole = new vscode.Range(document.positionAt(0), document.positionAt(before.length));
    return [vscode.TextEdit.replace(whole, after)];
  }
}

function logConfiguration(): void {
  const configuration = vscode.workspace.getConfiguration("dixtra");
  const configuredPath = configuration.get<string>("executablePath", "");
  const extraArguments = configuration.get<string[]>("arguments", []);

  channel.info(
    `configuration: executablePath ${configuredPath === "" ? "(none)" : configuredPath}, ` +
      `arguments ${JSON.stringify(extraArguments)}`,
  );
}

async function formatFile(file: string): Promise<FormatResult> {
  const configuration = vscode.workspace.getConfiguration("dixtra");
  const configuredPath = configuration.get<string>("executablePath", "");
  const extraArguments = configuration.get<string[]>("arguments", []);

  const platform =
    process.platform === "win32"
      ? "win32-x64"
      : process.platform === "darwin"
        ? process.arch === "arm64"
          ? "darwin-arm64"
          : "darwin-x64"
        : "linux-x64";
  const executableName = platform === "win32-x64" ? "dixtra.exe" : "dixtra";
  const bundled = path.join(extensionPath, "bin", platform, executableName);

  const candidates: string[] = [];
  if (configuredPath !== "") {
    candidates.push(configuredPath);
  }
  if (await exists(bundled)) {
    candidates.push(bundled);
  }
  candidates.push("dixtra");

  channel.debug(
    `bundled ${bundled} ${(await exists(bundled)) ? "exists" : "is missing"}, ` +
      `candidates ${JSON.stringify(candidates)}`,
  );

  let resolved: string | null = null;
  let exitCode: number | null = null;
  let failures = "";

  for (const candidate of candidates) {
    const args = [...extraArguments, file];
    channel.debug(`running ${candidate} ${JSON.stringify(args)}`);

    const outcome = await run(candidate, args);

    if (outcome.error && outcome.error.code !== "ENOENT") {
      const message = `could not start ${candidate}: ${outcome.error.message}`;
      channel.error(message);
      return { kind: "failed", message };
    }

    if (outcome.error) {
      channel.debug(`${candidate} not found, trying the next candidate`);
      continue;
    }

    resolved = candidate;
    exitCode = outcome.code;
    failures = outcome.stderr;
    break;
  }

  if (resolved === null) {
    const message = "dixtra executable not found (install the extension's binaries or set dixtra.executablePath)";
    channel.error(message);
    return { kind: "failed", message };
  }

  channel.info(`using ${resolved}${await versionOf(resolved)}`);
  channel.debug(
    `exit code ${exitCode}${failures === "" ? "" : `, stderr ${JSON.stringify(failures)}`}`,
  );

  if (exitCode === 0) {
    return { kind: "ok" };
  }

  if (exitCode === null) {
    return { kind: "failed", message: `could not start ${resolved}: ${failures}` };
  }

  return { kind: "failed", message: failures || `exit code ${exitCode}` };
}

async function versionOf(executable: string): Promise<string> {
  if (binaryVersion === null) {
    const outcome = await run(executable, ["--version"]);
    const version = outcome.stdout.split("\n")[0]?.trim() ?? "";
    binaryVersion = version === "" ? "" : ` ${version}`;
    channel.debug(`version probe of ${executable}: ${JSON.stringify(outcome)}`);
  }
  return binaryVersion;
}

async function run(
  executablePath: string,
  args: string[],
): Promise<{ code: number | null; error?: NodeJS.ErrnoException; stdout: string; stderr: string }> {
  const child = spawn(executablePath, args, {
    stdio: ["ignore", "pipe", "pipe"],
  });

  const out: Buffer[] = [];
  const err: Buffer[] = [];
  child.stdout.on("data", chunk => out.push(chunk));
  child.stderr.on("data", chunk => err.push(chunk));

  const outcome = await new Promise<{ code: number | null; error?: NodeJS.ErrnoException }>(resolve => {
    child.on("error", error => resolve({ code: null, error }));
    child.on("close", code => resolve({ code }));
  });

  return {
    ...outcome,
    stdout: Buffer.concat(out).toString().trim(),
    stderr: Buffer.concat(err).toString().trim(),
  };
}

async function exists(file: string): Promise<boolean> {
  try {
    await fs.stat(file);
    return true;
  } catch {
    return false;
  }
}
