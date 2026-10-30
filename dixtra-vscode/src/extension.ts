import * as vscode from "vscode";
import * as fs from "node:fs/promises";
import { spawn } from "node:child_process";

// The languages the formatter is offered on: `Shift+Alt+F` (Format Document) only — the extension never formats on
// save and never formats by itself.
const LANGUAGES = ["scala"];

export function activate(context: vscode.ExtensionContext): void {
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
      return [];
    }

    const whole = new vscode.Range(document.positionAt(0), document.positionAt(before.length));
    return [vscode.TextEdit.replace(whole, after)];
  }
}

// The seam: the only place a dixtra process is spawned, so a long-lived `dixtra --loop` child can replace it without
// touching the provider above.
async function formatFile(path: string): Promise<FormatResult> {
  const configuration = vscode.workspace.getConfiguration("dixtra");
  const executablePath = configuration.get<string>("executablePath", "dixtra");
  const extraArguments = configuration.get<string[]>("arguments", []);

  const child = spawn(executablePath, [...extraArguments, path], {
    stdio: ["ignore", "ignore", "pipe"],
  });
  
  const failures: Buffer[] = [];
  child.stderr.on("data", chunk => failures.push(chunk));

  const code: number | null = await new Promise((resolve, reject) => {
    child.on("error", reject);
    child.on("close", resolve);
  });

  if (code === 0) {
    return { 
      kind: "ok" 
    };
  }

  if (code === null) {
    return { 
      kind: "failed", 
      message: `could not start ${executablePath}: ${Buffer.concat(failures).toString().trim()}` 
    };
  }

  const message = Buffer.concat(failures).toString().trim() || `exit code ${code}`;
  return { 
    kind: "failed", 
    message 
  };
}
