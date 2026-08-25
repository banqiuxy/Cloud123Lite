package com.banqiu.thirdparty123pan.ui.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val RECYCLE_BIN = "recycle_bin"
    const val SHARES = "shares"
    const val ABOUT = "about"

    const val FILE_DETAIL = "file_detail/{fileId}?name={name}&size={size}&modTime={modTime}&parentId={parentId}&isFolder={isFolder}"
    const val PREVIEW = "preview/{fileId}?name={name}"

    fun fileDetail(
        fileId: Long,
        name: String,
        size: Long = 0,
        modTime: Long = 0,
        parentId: Long = 0,
        isFolder: Boolean = false
    ): String = "file_detail/$fileId?name=${Uri.encode(name)}&size=$size&modTime=$modTime&parentId=$parentId&isFolder=$isFolder"

    fun preview(fileId: Long, name: String): String =
        "preview/$fileId?name=${Uri.encode(name)}"
}