<template>
  <div class="schedule-board">
    <div
      v-for="item in items"
      :key="`${item.date}-${item.staffName}`"
      class="shift-card clickable"
      :class="{ 'over-limit': item.overLimit }"
      :style="{ borderColor: item.color }"
      @click="openDialog(item)"
    >
      <div class="date">{{ item.date }}</div>
      <strong>{{ item.staffName }}</strong>
      <span>{{ item.position }} · {{ item.shift }}</span>
      <div class="flags">
        <el-tag v-if="item.holiday" type="danger" size="small">特殊日期</el-tag>
        <el-tag v-if="item.overLimit" type="danger" size="small">连续超5天</el-tag>
        <el-tag v-if="item.manual" type="warning" size="small">已手动调整</el-tag>
      </div>
    </div>
  </div>

  <el-dialog v-model="dialogVisible" title="调整班次" width="360px">
    <p v-if="selected" class="dialog-tip">{{ selected.date }} · {{ selected.staffName }}（当前：{{ selected.shift }}）</p>
    <el-radio-group v-model="pickedShift">
      <el-radio-button v-for="shift in SHIFT_OPTIONS" :key="shift" :value="shift">{{ shift }}</el-radio-button>
    </el-radio-group>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!selected || pickedShift === selected.shift" @click="confirm">确定换班</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { ScheduleItem } from '../types/schedule';

const SHIFT_OPTIONS = ['白班', '中班', '夜班', '休息'];

defineProps<{ items: ScheduleItem[] }>();
const emit = defineEmits<{ adjust: [item: ScheduleItem, shift: string] }>();

const dialogVisible = ref(false);
const selected = ref<ScheduleItem | null>(null);
const pickedShift = ref('白班');

function openDialog(item: ScheduleItem) {
  selected.value = item;
  pickedShift.value = item.shift;
  dialogVisible.value = true;
}

function confirm() {
  if (selected.value) {
    emit('adjust', selected.value, pickedShift.value);
  }
  dialogVisible.value = false;
}
</script>

<style scoped>
.clickable {
  cursor: pointer;
}

.clickable:hover {
  background: #eef4fc;
}

.over-limit {
  background: #fef0f0;
}

.flags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.dialog-tip {
  margin-top: 0;
  color: #66768a;
}
</style>
