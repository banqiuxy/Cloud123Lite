package com.banqiu.thirdparty123pan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.banqiu.thirdparty123pan.ui.components.*
import com.banqiu.thirdparty123pan.ui.theme.Cloud123Theme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** Debug-only visual fixture: no login, no storage or transfer operations, separate install via -PglassPreview. */
class GlassPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dark = intent.getBooleanExtra("dark", false)
        setContent {
            Cloud123Theme(darkTheme = dark, dynamicColor = false) {
                GlassScene(true, Modifier.fillMaxSize()) { Preview() }
            }
        }
    }
}

@Composable
private fun Preview() {
    var tab by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var filter by remember { mutableIntStateOf(0) }
    var toggle by remember { mutableStateOf(true) }
    val backdrop = rememberLayerBackdrop()
    val labels = listOf("首页", "传输", "相册", "我的")
    val icons = listOf(Icons.Default.Folder, Icons.Default.Sync, Icons.Default.PhotoLibrary, Icons.Default.Person)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            CloudBackground(null)
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 16.dp)) {
                Text("玻璃组件验收", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge)
                Text("当前页面：${labels[tab]} · 切换次数：$count", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("全部", "图片", "视频", "文档", "音频").forEachIndexed { index, label ->
                        AppFilterChip(filter == index, { filter = index }, { Text(label, maxLines = 1) }, Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("液态玻璃", color = MaterialTheme.colorScheme.onSurface)
                    LiquidGlassSwitch(toggle, { toggle = it }, LocalGlassBackdrop.current!!, true, "液态玻璃开关")
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 0.dp)) {
                    items(18) { index ->
                        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).background(listOf(Color(0xFF237FD0), Color(0xFFE9983A), Color(0xFF6D58CA))[index % 3], RoundedCornerShape(10.dp)))
                            Text("示例文件 ${index + 1} — 对比采样", Modifier.weight(1f).padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
                            AppIconButton({}) { Icon(Icons.Default.MoreVert, "更多") }
                        }
                    }
                }
            }
        }
        GlassFab(Icons.Default.Upload, "上传", {}, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 112.dp))
        GlassBottomBar(backdrop, labels.mapIndexed { i, label -> BottomNavItem(label, icons[i], icons[i]) }, tab,
            { if (it != tab) { tab = it; count++ } }, Modifier.align(Alignment.BottomCenter))
    }
}
