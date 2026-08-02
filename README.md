# Inline Review Notes for AI

![Build](https://github.com/naoyukik/intellij-plugin-inline-review-notes-for-ai/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/32674.svg)](https://plugins.jetbrains.com/plugin/32674)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/32674.svg)](https://plugins.jetbrains.com/plugin/32674)

An IntelliJ Platform plugin that lets you add inline review comments to local files with the same UX as GitHub PR code reviews.

Comments are saved to `.inline-review-notes/{branch}.json` on a per-branch basis. AI tools can read this JSON to instantly understand your review notes without cluttering source files.

## Purpose

- Record review notes for AI while reading code, right in the editor
- Eliminate verbal explanations and copy-pasting, reducing review cycle time
- Enable AI to reconstruct context using only `filePath` and line numbers

## Features

- Gutter add-button appears when selecting a line or line range
- Inline comment display after input
- Click existing comments to re-edit or delete
- Per-branch persistence: `.inline-review-notes/<branch>.json` (file is created automatically using the current branch name)
- Automatically reload comments when their storage file is changed or deleted
- Automatically reload comments and show a notification when switching branches
- Track comment line positions when lines are inserted or deleted in the IntelliJ editor, and synchronize the updated line numbers to JSON on save
- Comments with `resolvedAt` are treated as resolved

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Save comment | <kbd>Ctrl+Enter</kbd> (Windows/Linux) / <kbd>Cmd+Enter</kbd> (macOS) |
| Move focus to next field | <kbd>Tab</kbd> |
| Move focus to previous field | <kbd>Shift+Tab</kbd> |

## Installation

<kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > Search for "Inline Review Notes for AI".

Or download from [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32674) and install manually.

## Usage

1. Select a line or line range in the editor
2. Click the **gutter plus button** or right-click and select **Add Inline Review Notes**
3. Enter your comment in the panel and click **Save**
4. The comment is saved to `.inline-review-notes/{branch}.json` (using the current branch name)
5. Use an AI agent skill or prompt to load the JSON and process the comments
6. Resolved comments are hidden from the editor

## Data Format

Comments are stored in `.inline-review-notes/<branch>.json`.

- `filePath` is relative to the project root
- Resolution status is expressed by the presence or absence of `resolvedAt`
- The `.inline-review-notes/` directory is excluded from version control (`.gitignore` recommended)

## Limitations

- Line tracking applies to edits made in the IntelliJ editor. Changes to source files made externally, such as by another application or a Git operation, are not supported for line tracking.

## Recommended: AI Agent Skills

To get the most out of this plugin, we recommend configuring AI agent skills that automate comment processing.

### Example: Load and Process Comments

```
@./.inline-review-notes/{current_branch}.json and follow the comment instructions. For resolved items, add resolvedAt with the current date.
```

### Example: Clean Up Stale Comment Files

```
@./.inline-review-notes/ and compare with local git branches. Delete files for branches that no longer exist.
```
