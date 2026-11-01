import * as vscode from "vscode";
import * as path from "node:path";
import * as fs from "node:fs/promises";
import { spawn } from "node:child_process";

// The languages the formatter is offered on: `Shift+Alt+F` (Format Document) only — the extension never formats on
// save and never formats by itself.
const LANGUAGES = ["scala"];

// The folder the extension is installed in and the log of its runs: both are set on activation.
let extensionPath = "";
let channel: vscode.OutputChannel;

export function activate(context: vscode.ExtensionContext): void {
  extensionPath = context.extensionPath;
  channel = vscode.window.createOutputChannel("dixtra");
  context.subscriptions.push(channel);

  const provider = new DixtraFormatter();
  context.subscriptions.push(
    vscode.languages.registerDocumentFormattingEditProvider(LANGUAGES, provider),
  );
}

type FormatResult =
  | { kind: "ok" }
  | { kind: "failed"; message: string };

class DixtraFormatter implements vscode.DocumentFormattingEditProvider {
  async provideDocumentFormattingEdits(
    document: vscode.TextDocument,
  ): Promise<vscode.TextEdit[]> {
    if (document.uri.scheme !== "file") {
      void vscode.window.showInformationMessage("dixtra formats files on disk: save this document first.");
      return [];
    }

    if (document.isDirty && !(await document.save())) {
      void vscode.window.showErrorMessage("dixtra: the document could not be saved.");
      return [];
    }

    const result = await formatFile(document.uri.fsPath);
    if (result.kind === "failed") {
      void vscode.window.showErrorMessage(`dixtra: ${result.message}`);
      return [];
    }

    const before = document.getText();
    const after = await fs.readFile(document.uri.fsPath, "utf8");
    if (after === before) {
      channel.appendLine(`dixtra: no edits for ${document.uri.fsPath}`);
      return [];
    }

    channel.appendLine(`dixtra: edits produced for ${document.uri.fsPath}`);

    const whole = new vscode.Range(document.positionAt(0), document.positionAt(before.length));
    return [vscode.TextEdit.replace(whole, after)];
  }
}

// The seam: the only place a dixtra process is spawned, so a long-lived `dixtra --loop` child can replace it without
// touching the provider above. The candidates are tried in order of preference and the first one that starts is the
// one used: the configured executable, the bundled one for the platform, then `dixtra` from the `PATH`.
async function formatFile(file: string): Promise<FormatResult> {
  const configuration = vscode.workspace.getConfiguration("dixtra");
  const configuredPath = configuration.get<string>("executablePath", "");
  const extraArguments = configuration.get<string[]>("arguments", []);

  const platform = process.platform === "win32" ? "win32-x64" : "linux-x64";
  const executableName = process.platform === "win32" ? "dixtra.exe" : "dixtra";
  const bundled = path.join(extensionPath, "bin", platform, executableName);

  const candidates: string[] = [];
  if (configuredPath !== "") {
    candidates.push(configuredPath);
  }
  if (await exists(bundled)) {
    candidates.push(bundled);
  }
  candidates.push("dixtra");

  let resolved: string | null = null;
  let exitCode: number | null = null;
  let failures = "";

  for (const candidate of candidates) {
    channel.appendLine(`dixtra: formatting ${file} with ${candidate}`);

    const outcome = await run(candidate, [...extraArguments, file]);

    if (outcome.error && outcome.error.code !== "ENOENT") {
      const message = `could not start ${candidate}: ${outcome.error.message}`;
      channel.appendLine(message);
      return { kind: "failed", message };
    }

    if (outcome.error) {
      continue;
    }

    resolved = candidate;
    exitCode = outcome.code;
    failures = outcome.stderr;
    break;
  }

  if (resolved === null) {
    const message = "dixtra executable not found (install the extension's binaries or set dixtra.executablePath)";
    channel.appendLine(message);
    return { kind: "failed", message };
  }

  channel.appendLine(`dixtra: ${resolved} on ${file} exited with code ${exitCode}`);

  if (exitCode === 0) {
    return { kind: "ok" };
  }

  if (exitCode === null) {
    return { kind: "failed", message: `could not start ${resolved}: ${failures}` };
  }

  return { kind: "failed", message: failures || `exit code ${exitCode}` };
}

// One run of dixtra: the error of the spawn itself, or the exit code and the stderr of the process
async function run(
  executablePath: string,
  args: string[],
): Promise<{ code: number | null; error?: NodeJS.ErrnoException; stderr: string }> {
  const child = spawn(executablePath, args, {
    stdio: ["ignore", "ignore", "pipe"],
  });

  const chunks: Buffer[] = [];
  child.stderr.on("data", chunk => chunks.push(chunk));

  const outcome = await new Promise<{ code: number | null; error?: NodeJS.ErrnoException }>(resolve => {
    child.on("error", error => resolve({ code: null, error }));
    child.on("close", code => resolve({ code }));
  });

  return { ...outcome, stderr: Buffer.concat(chunks).toString().trim() };
}

async function exists(file: string): Promise<boolean> {
  try {
    await fs.stat(file);
    return true;
  } catch {
    return false;
  }
}
