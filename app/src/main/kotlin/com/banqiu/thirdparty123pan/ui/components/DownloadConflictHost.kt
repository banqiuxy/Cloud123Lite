package com.banqiu.thirdparty123pan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.domain.model.TaskStatus
import com.banqiu.thirdparty123pan.domain.repository.TransferRepository
import com.banqiu.thirdparty123pan.data.transfer.validateDownloadName

@dagger.hilt.android.lifecycle.HiltViewModel
class DownloadConflictHolder @javax.inject.Inject constructor(val repository: TransferRepository) : androidx.lifecycle.ViewModel()

@Composable
fun DownloadConflictHost(holder: DownloadConflictHolder = hiltViewModel()) {
    val tasks by holder.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val state by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (!state.isAtLeast(Lifecycle.State.RESUMED)) return
    val task = tasks.filter { it.status == TaskStatus.CONFLICT }.minByOrNull { it.id } ?: return
    key(task.id) {
        var rename by remember { mutableStateOf(false) }
        var name by remember { mutableStateOf(task.name) }
        var error by remember { mutableStateOf<String?>(null) }
        AppAlertDialog(
            onDismissRequest = {},
            title = { Text("下载文件已存在") },
            text = {
                Column {
                    Text("“${task.name}”与目标目录中的文件同名。此任务已暂停，其他下载不受影响。")
                    if (rename) AppOutlinedTextField(value = name, onValueChange = { name = it; error = null },
                        label = { Text("新文件名") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = error != null)
                    (error ?: task.error)?.let { Text(it) }
                }
            },
            confirmButton = {
                AppTextButton(onClick = {
                    if (rename) {
                        try { validateDownloadName(name); holder.repository.resolveConflict(task.id, false, name) }
                        catch (e: IllegalArgumentException) { error = e.message }
                    } else holder.repository.resolveConflict(task.id, true)
                }) { Text(if (rename) "保存新名称" else "确认覆盖") }
            },
            dismissButton = {
                AppTextButton(onClick = { rename = !rename }) { Text(if (rename) "返回" else "重命名") }
                AppTextButton(onClick = { holder.repository.cancel(task.id) }) { Text("取消任务") }
            }
        )
    }
}
