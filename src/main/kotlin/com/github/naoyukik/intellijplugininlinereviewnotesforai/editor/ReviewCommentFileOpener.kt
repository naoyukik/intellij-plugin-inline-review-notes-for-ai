package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile


class ReviewCommentFileOpener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        // ここに処理を書く
        println("Opened: " + file.getPath())
        // 任意の処理
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        // ここに処理を書く
        println("Closed: " + file.getPath())
        // 任意の処理
    }
}