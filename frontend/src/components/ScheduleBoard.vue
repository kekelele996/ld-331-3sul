<template>
  <div class="schedule-matrix-wrapper">
    <div class="schedule-matrix">
      <div class="matrix-header corner">人员 \ 日期</div>
      <div
        v-for="date in dates"
        :key="date.iso"
        class="matrix-header date-head"
        :class="{ holiday: date.holiday }"
      >
        <span>{{ date.label }}</span>
        <small>{{ date.weekday }}</small>
        <el-tag v-if="date.holiday" type="danger" size="small" effect="dark">特殊</el-tag>
      </div>

      <template v-for="staff in staffRows" :key="staff.name">
        <div class="matrix-row-head">
          <strong>{{ staff.name }}</strong>
          <small>{{ staff.position }}</small>
        </div>
        <div
          v-for="cell in staff.cells"
          :key="`${cell.date}-${cell.staffName}`"
          class="shift-cell"
          :class="cellClass(cell)"
          :style="{ borderLeftColor: cell.color }"
          :title="cellTitle(cell)"
          @click="openDialog(cell)"
        >
          <span class="shift-name">{{ cell.shift }}</span>
          <el-tooltip v-if="cell.adjusted" content="护士长手动调整，一键生成时保留" placement="top">
            <span class="adjusted-mark">改</span>
          </el-tooltip>
          <el-tooltip v-if="cell.conflict !== 'none'" :content="conflictText(cell.conflict)" placement="top">
            <span class="conflict-mark">!</span>
          </el-tooltip>
        </div>
      </template>
    </div>

    <el-dialog v-model="dialogVisible" title="调整班次" width="380px">
      <div v-if="activeCell" class="adjust-dialog">
        <p class="adjust-target">
          {{ activeCell.date }} {{ activeCell.staffName }}（{{ activeCell.position }}）
        </p>
        <p class="adjust-old">原班次：<el-tag size="small">{{ activeCell.shift }}</el-tag></p>
        <el-radio-group v-model="newShift" class="shift-options">
          <el-radio v-for="shift in SHIFT_TYPES" :key="shift" :value="shift">
            <span :style="{ color: SHIFT_COLORS[shift] }">{{ shift }}</span>
          </el-radio>
        </el-radio-group>
        <el-input
          v-model="reason"
          type="textarea"
          :rows="2"
          maxlength="80"
          show-word-limit
          placeholder="调整原因（选填）"
        />
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存并重算冲突/工时</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { adjustShift } from '../api/schedule';
import { SHIFT_COLORS, SHIFT_TYPES } from '../constants/app';
import type { DashboardData, ScheduleItem } from '../types/schedule';

const props = defineProps<{ items: ScheduleItem[]; department: string }>();
const emit = defineEmits<{ updated: [data: DashboardData] }>();

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

const dates = computed(() => {
  const seen = new Map<string, boolean>();
  props.items.forEach((item) => {
    if (!seen.has(item.date)) {
      seen.set(item.date, item.holiday);
    } else if (item.holiday) {
      seen.set(item.date, true);
    }
  });
  return Array.from(seen.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([iso, holiday]) => {
      const day = new Date(`${iso}T00:00:00`);
      return {
        iso,
        label: iso.slice(5),
        weekday: WEEKDAYS[day.getDay()],
        holiday,
      };
    });
});

const staffRows = computed(() => {
  const map = new Map<string, { name: string; position: string; cells: ScheduleItem[] }>();
  props.items.forEach((item) => {
    if (!map.has(item.staffName)) {
      map.set(item.staffName, { name: item.staffName, position: item.position, cells: [] });
    }
    map.get(item.staffName)!.cells.push(item);
  });
  map.forEach((row) => row.cells.sort((a, b) => a.date.localeCompare(b.date)));
  return Array.from(map.values());
});

/** 每个人截至当天的连续上班天数（休息打断、不计入），用于悬浮提示。 */
const streaks = computed(() => {
  const result = new Map<string, number>();
  staffRows.value.forEach((row) => {
    let count = 0;
    row.cells.forEach((cell) => {
      if (cell.shift === '休息') {
        count = 0;
      } else {
        count += 1;
      }
      result.set(`${cell.staffName}@${cell.date}`, count);
    });
  });
  return result;
});

function cellClass(cell: ScheduleItem) {
  return {
    'cell-adjusted': cell.adjusted,
    'cell-night-day': cell.conflict === 'nightToDay',
    'cell-streak': cell.conflict === 'continuousDays',
  };
}

function conflictText(conflict: string) {
  if (conflict === 'nightToDay') return '冲突：夜班后直接接白班';
  if (conflict === 'continuousDays') return '冲突：连续上班超过 5 天';
  return '';
}

function cellTitle(cell: ScheduleItem) {
  const parts = [
    `${cell.date} ${cell.staffName}：${cell.shift}`,
    `连续上班 ${streaks.value.get(`${cell.staffName}@${cell.date}`) ?? 0} 天（休息不计入）`,
    '点击调整班次',
  ];
  if (cell.conflict !== 'none') parts.push(conflictText(cell.conflict));
  return parts.join('\n');
}

const dialogVisible = ref(false);
const saving = ref(false);
const activeCell = ref<ScheduleItem | null>(null);
const newShift = ref('白班');
const reason = ref('');

function openDialog(cell: ScheduleItem) {
  activeCell.value = cell;
  newShift.value = cell.shift;
  reason.value = '';
  dialogVisible.value = true;
}

async function save() {
  if (!activeCell.value) return;
  if (newShift.value === activeCell.value.shift) {
    dialogVisible.value = false;
    return;
  }
  saving.value = true;
  try {
    const data = await adjustShift({
      department: props.department,
      date: activeCell.value.date,
      staffName: activeCell.value.staffName,
      newShift: newShift.value,
      reason: reason.value.trim() || undefined,
    });
    emit('updated', data);
    dialogVisible.value = false;
    ElMessage.success('班次已调整，冲突与工时已重新计算');
  } catch (error: unknown) {
    const message =
      typeof error === 'object' && error !== null && 'response' in error
        ? ((error as { response?: { data?: { message?: string } } }).response?.data?.message ?? '调整失败')
        : '调整失败';
    ElMessage.error(message);
  } finally {
    saving.value = false;
  }
}
</script>
