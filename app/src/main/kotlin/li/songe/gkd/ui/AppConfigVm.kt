package li.songe.gkd.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import li.songe.gkd.data.SubsConfig
import li.songe.gkd.db.DbSet
import li.songe.gkd.ui.destinations.AppConfigPageDestination
import li.songe.gkd.util.collator
import li.songe.gkd.util.ruleSummaryFlow
import javax.inject.Inject

@HiltViewModel
class AppConfigVm @Inject constructor(stateHandle: SavedStateHandle) : ViewModel() {
    private val args = AppConfigPageDestination.argsFrom(stateHandle)

    private val latestGlobalLogsFlow = DbSet.clickLogDao.queryAppLatest(
        args.appId,
        SubsConfig.GlobalGroupType
    )
    private val latestAppLogsFlow = DbSet.clickLogDao.queryAppLatest(
        args.appId,
        SubsConfig.AppGroupType
    )

    val ruleSortTypeFlow = MutableStateFlow<RuleSortType>(RuleSortType.Default)

    val globalGroupsFlow = combine(
        ruleSummaryFlow.map { r -> r.globalGroups },
        ruleSortTypeFlow,
        latestGlobalLogsFlow
    ) { list, type, logs ->
        when (type) {
            RuleSortType.Default -> list
            RuleSortType.ByName -> list.sortedWith { a, b ->
                collator.compare(
                    a.group.name,
                    b.group.name
                )
            }

            RuleSortType.ByTime -> list.sortedBy { a ->
                -(logs.find { c -> c.groupKey == a.group.key && c.subsId == a.subsItem.id }?.id
                    ?: 0)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appGroupsFlow = combine(
        ruleSummaryFlow.map { r -> r.appIdToAllGroups[args.appId] ?: emptyList() },
        ruleSortTypeFlow,
        latestAppLogsFlow
    ) { list, type, logs ->
        when (type) {
            RuleSortType.Default -> list
            RuleSortType.ByName -> list.sortedWith { a, b ->
                collator.compare(
                    a.group.name,
                    b.group.name
                )
            }

            RuleSortType.ByTime -> list.sortedBy { a ->
                -(logs.find { c -> c.groupKey == a.group.key && c.subsId == a.subsItem.id }?.id
                    ?: 0)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

}

sealed class RuleSortType(val value: Int, val label: String) {
    data object Default : RuleSortType(0, "按订阅顺序")
    data object ByTime : RuleSortType(1, "按触发时间")
    data object ByName : RuleSortType(2, "按名称")

    companion object {
        val allSubObject by lazy { arrayOf(Default, ByTime, ByName) }
    }
}