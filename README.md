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
- Per-branch persistence: `.inline-review-notes/<branch>.json`
- Comments with `resolvedAt` are treated as resolved

## Installation

<kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > Search for "Inline Review Notes for AI".

Or download from [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32674) and install manually.

## Data Format

Comments are stored in `.inline-review-notes/<branch>.json`.

- `filePath` is relative to the project root
- Resolution status is expressed by the presence or absence of `resolvedAt`
- The `.inline-review-notes/` directory is excluded from version control (`.gitignore` recommended)
