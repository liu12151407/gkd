package li.songe.gkd.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.coroutines.Dispatchers
import li.songe.gkd.data.CategoryConfig
import li.songe.gkd.data.RawSubscription
import li.songe.gkd.db.DbSet
import li.songe.gkd.ui.component.getDialogResult
import li.songe.gkd.ui.home.enableGroupRadioOptions
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.ProfileTransitions
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.toast
import li.songe.gkd.util.updateSubscription

@RootNavGraph
@Destination(style = ProfileTransitions::class)
@Composable
fun CategoryPage(subsItemId: Long) {
    val navController = LocalNavController.current
    val vm = hiltViewModel<CategoryVm>()
    val subsItem by vm.subsItemFlow.collectAsState()
    val subsRaw by vm.subsRawFlow.collectAsState()
    val categoryConfigs by vm.categoryConfigsFlow.collectAsState()
    val editable = subsItem != null && subsItemId < 0

    var showAddDlg by remember {
        mutableStateOf(false)
    }
    var editEnableCategory by remember {
        mutableStateOf<RawSubscription.RawCategory?>(null)
    }
    val (editNameCategory, setEditNameCategory) = remember {
        mutableStateOf<RawSubscription.RawCategory?>(null)
    }

    val categories = subsRaw?.categories ?: emptyList()
    val categoriesGroups = subsRaw?.categoryToGroupsMap ?: emptyMap()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(scrollBehavior = scrollBehavior, navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        }, title = { Text(text = "规则类别/${subsRaw?.name ?: subsItemId}") }, actions = {})
    }, floatingActionButton = {
        if (editable) {
            FloatingActionButton(onClick = { showAddDlg = true }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "add",
                )
            }
        }
    }) { contentPadding ->
        LazyColumn(
            modifier = Modifier.padding(contentPadding)
        ) {
            items(categories, { it.key }) { category ->
                Row(modifier = Modifier
                    .clickable {
                        editEnableCategory = category
                    }
                    .padding(10.dp, 6.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    val size = categoriesGroups[category]?.size ?: 0
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.name, fontSize = 18.sp
                        )
                        if (size > 0) {
                            Text(
                                text = "${size}规则组",
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "暂无规则",
                                fontSize = 14.sp,
                                color = LocalContentColor.current.copy(alpha = 0.5f)
                            )
                        }
                    }
                    if (editable) {
                        var expanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .wrapContentSize(Alignment.TopStart)
                        ) {
                            IconButton(onClick = {
                                expanded = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(text = "编辑")
                                    },
                                    onClick = {
                                        expanded = false
                                        setEditNameCategory(category)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(text = "删除", color = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        expanded = false
                                        vm.viewModelScope.launchTry {
                                            val result = getDialogResult(
                                                "删除类别",
                                                "是否删除类别 ${category.name} ?"
                                            )
                                            if (!result) return@launchTry
                                            subsItem?.apply {
                                                updateSubscription(subsRaw!!.copy(
                                                    categories = subsRaw!!.categories.filter { c -> c.key != category.key }
                                                ))
                                                DbSet.subsItemDao.update(copy(mtime = System.currentTimeMillis()))
                                            }
                                            DbSet.categoryConfigDao.deleteByCategoryKey(
                                                subsItemId,
                                                category.key
                                            )
                                            toast("删除成功")
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categoryConfig =
                            categoryConfigs.find { c -> c.categoryKey == category.key }
                        val enable =
                            if (categoryConfig != null) categoryConfig.enable else category.enable
                        Text(
                            text = enableGroupRadioOptions.find { e -> e.second == enable }?.first
                                ?: "",
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                }
                if (categories.lastOrNull() !== category) {
                    HorizontalDivider()
                }
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
                if (categories.isEmpty()) {
                    Text(
                        text = "暂无类别",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (editable) {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    editEnableCategory?.let { category ->
        val categoryConfig =
            categoryConfigs.find { c -> c.categoryKey == category.key }
        val enable =
            if (categoryConfig != null) categoryConfig.enable else category.enable
        Dialog(onDismissRequest = { editEnableCategory = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                enableGroupRadioOptions.forEach { option ->
                    val onClick: () -> Unit = {
                        vm.viewModelScope.launchTry(Dispatchers.IO) {
                            DbSet.categoryConfigDao.insert(
                                (categoryConfig ?: CategoryConfig(
                                    enable = option.second,
                                    subsItemId = subsItemId,
                                    categoryKey = category.key
                                )).copy(enable = option.second)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (option.second == enable),
                                onClick = onClick
                            )
                            .padding(horizontal = 16.dp)
                    ) {
                        RadioButton(
                            selected = (option.second == enable),
                            onClick = onClick
                        )
                        Text(
                            text = option.first, modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }

    val subsRawVal = subsRaw
    if (editNameCategory != null && subsRawVal != null) {
        var source by remember {
            mutableStateOf(editNameCategory.name)
        }
        AlertDialog(
            title = { Text(text = "编辑类别") },
            text = {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = "请输入类别名称") },
                    singleLine = true
                )
            },
            onDismissRequest = { setEditNameCategory(null) },
            dismissButton = {
                TextButton(onClick = { setEditNameCategory(null) }) {
                    Text(text = "取消")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = source.isNotBlank() && source != editNameCategory.name,
                    onClick = {
                        if (categories.any { c -> c.key != editNameCategory.key && c.name == source }) {
                            toast("不可添加同名类别")
                            return@TextButton
                        }
                        vm.viewModelScope.launchTry(Dispatchers.IO) {
                            subsItem?.apply {
                                updateSubscription(subsRawVal.copy(
                                    categories = categories.toMutableList().apply {
                                        val i =
                                            categories.indexOfFirst { c -> c.key == editNameCategory.key }
                                        if (i >= 0) {
                                            set(i, editNameCategory.copy(name = source))
                                        }
                                    }
                                ))
                                DbSet.subsItemDao.update(copy(mtime = System.currentTimeMillis()))
                            }
                            toast("修改成功")
                            setEditNameCategory(null)
                        }
                    }
                ) {
                    Text(text = "确认")
                }
            }
        )
    }
    if (showAddDlg && subsRawVal != null) {
        var source by remember {
            mutableStateOf("")
        }
        AlertDialog(
            title = { Text(text = "添加类别") },
            text = {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = "请输入类别名称") },
                    singleLine = true
                )
            },
            onDismissRequest = { showAddDlg = false },
            dismissButton = {
                TextButton(onClick = { showAddDlg = false }) {
                    Text(text = "取消")
                }
            },
            confirmButton = {
                TextButton(enabled = source.isNotEmpty(), onClick = {
                    if (categories.any { c -> c.name == source }) {
                        toast("不可添加同名类别")
                        return@TextButton
                    }
                    showAddDlg = false
                    vm.viewModelScope.launchTry(Dispatchers.IO) {
                        subsItem?.apply {
                            updateSubscription(subsRawVal.copy(
                                categories = categories.toMutableList().apply {
                                    add(RawSubscription.RawCategory(
                                        key = (categories.maxOfOrNull { c -> c.key } ?: -1) + 1,
                                        name = source,
                                        enable = null
                                    ))
                                }
                            ))
                            DbSet.subsItemDao.update(copy(mtime = System.currentTimeMillis()))
                            toast("添加成功")
                        }
                    }
                }) {
                    Text(text = "确认")
                }
            }
        )
    }
}