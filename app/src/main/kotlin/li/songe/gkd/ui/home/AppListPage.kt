package li.songe.gkd.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.flow.update
import li.songe.gkd.ui.component.AppBarTextField
import li.songe.gkd.ui.destinations.AppConfigPageDestination
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.SortTypeOption
import li.songe.gkd.util.navigate
import li.songe.gkd.util.ruleSummaryFlow
import li.songe.gkd.util.storeFlow

val appListNav = BottomNavItem(
    label = "应用", icon = Icons.Default.Apps
)

@Composable
fun useAppListPage(): ScaffoldExt {
    val navController = LocalNavController.current

    val vm = hiltViewModel<HomeVm>()
    val showSystemApp by vm.showSystemAppFlow.collectAsState()
    val showHiddenApp by vm.showHiddenAppFlow.collectAsState()
    val sortType by vm.sortTypeFlow.collectAsState()
    val orderedAppInfos by vm.appInfosFlow.collectAsState()
    val searchStr by vm.searchStrFlow.collectAsState()
    val ruleSummary by ruleSummaryFlow.collectAsState()

    val globalDesc = if (ruleSummary.globalGroups.isNotEmpty()) {
        "${ruleSummary.globalGroups.size}全局"
    } else {
        null
    }

    var expanded by remember { mutableStateOf(false) }
    var showSearchBar by rememberSaveable {
        mutableStateOf(false)
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = showSearchBar, block = {
        if (showSearchBar && searchStr.isEmpty()) {
            focusRequester.requestFocus()
        }
        if (!showSearchBar) {
            vm.searchStrFlow.value = ""
        }
    })
    val listState = rememberLazyListState()

    var isFirstVisit by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = orderedAppInfos, block = {
        if (isFirstVisit) {
            listState.scrollToItem(0)
        } else {
            isFirstVisit = true
        }
    })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    return ScaffoldExt(navItem = appListNav,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(scrollBehavior = scrollBehavior, title = {
                if (showSearchBar) {
                    AppBarTextField(
                        value = searchStr,
                        onValueChange = { newValue -> vm.searchStrFlow.value = newValue.trim() },
                        hint = "请输入应用名称/ID",
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                } else {
                    Text(
                        text = appListNav.label,
                    )
                }
            }, actions = {
                if (showSearchBar) {
                    IconButton(onClick = {
                        if (vm.searchStrFlow.value.isEmpty()) {
                            showSearchBar = false
                        } else {
                            vm.searchStrFlow.value = ""
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null
                        )
                    }
                } else {
                    IconButton(onClick = {
                        showSearchBar = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = {
                        expanded = true
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null
                        )
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.TopStart)
                    ) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            SortTypeOption.allSubObject.forEach { sortOption ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = sortType == sortOption,
                                                onClick = {
                                                    storeFlow.update { s -> s.copy(sortType = sortOption.value) }
                                                }
                                            )
                                            Text(sortOption.label)
                                        }
                                    },
                                    onClick = {
                                        storeFlow.update { s -> s.copy(sortType = sortOption.value) }
                                    },
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = showSystemApp,
                                            onCheckedChange = {
                                                storeFlow.update { s -> s.copy(showSystemApp = !showSystemApp) }
                                            })
                                        Text("显示系统应用")
                                    }
                                },
                                onClick = {
                                    storeFlow.update { s -> s.copy(showSystemApp = !showSystemApp) }
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = showHiddenApp,
                                            onCheckedChange = {
                                                storeFlow.update { s -> s.copy(showHiddenApp = !s.showHiddenApp) }
                                            })
                                        Text("显示隐藏应用")
                                    }
                                },
                                onClick = {
                                    storeFlow.update { s -> s.copy(showHiddenApp = !showHiddenApp) }
                                },
                            )
                        }
                    }
                }
            })
        }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            state = listState
        ) {
            items(orderedAppInfos, { it.id }) { appInfo ->
                Row(
                    modifier = Modifier
                        .clickable {
                            navController.navigate(AppConfigPageDestination(appInfo.id))
                        }
                        .height(IntrinsicSize.Min)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (appInfo.icon != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        ) {
                            Image(
                                painter = rememberDrawablePainter(appInfo.icon),
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxHeight()
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = appInfo.name,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val appGroups = ruleSummary.appIdToAllGroups[appInfo.id] ?: emptyList()

                        val appDesc = if (appGroups.isNotEmpty()) {
                            when (val disabledCount = appGroups.count { g -> !g.enable }) {
                                0 -> {
                                    "${appGroups.size}组规则"
                                }

                                appGroups.size -> {
                                    "${appGroups.size}组规则/${disabledCount}关闭"
                                }

                                else -> {
                                    "${appGroups.size}组规则/${appGroups.size - disabledCount}启用/${disabledCount}关闭"
                                }
                            }
                        } else {
                            null
                        }

                        val desc = if (globalDesc != null) {
                            if (appDesc != null) {
                                "$globalDesc/$appDesc"
                            } else {
                                globalDesc
                            }
                        } else {
                            appDesc
                        }

                        if (desc != null) {
                            Text(text = desc)
                        } else {
                            Text(
                                text = "暂无规则",
                                color = LocalContentColor.current.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
                if (orderedAppInfos.isEmpty() && searchStr.isNotEmpty()) {
                    Text(
                        text = "暂无搜索结果",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}