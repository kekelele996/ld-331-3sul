<template>
  <el-container class="page">
    <el-header class="topbar">
      <h1>{{ APP_TITLE }}</h1>
      <el-select v-model="department" @change="load">
        <el-option label="急诊科" value="急诊科" />
        <el-option label="心内科" value="心内科" />
      </el-select>
    </el-header>
    <el-main class="main">
      <el-alert title="规则引擎已加载：连续工作上限、周末轮循、夜班后禁接白班、节假日优先级。点击排班卡片可直接换班。" type="info" show-icon />
      <section class="grid">
        <el-card shadow="never">
          <template #header>排班规则</template>
          <el-tag v-for="rule in data.rules" :key="rule" class="tag">{{ rule }}</el-tag>
        </el-card>
        <el-card shadow="never">
          <template #header>冲突检测</template>
          <el-empty v-if="data.conflicts.length === 0" description="暂无冲突" :image-size="60" />
          <el-timeline v-else>
            <el-timeline-item v-for="alert in data.conflicts" :key="`${alert.staffName}-${alert.date}-${alert.message}`" :type="alert.level === 'danger' ? 'danger' : 'warning'">
              {{ alert.date }} {{ alert.staffName }}：{{ alert.message }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </section>

      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>可视化排班表（点击卡片换班）</span>
            <el-button type="primary" :loading="generating" @click="onGenerate">一键生成</el-button>
          </div>
        </template>
        <ScheduleBoard :items="data.schedule" @adjust="onAdjust" />
      </el-card>

      <section class="grid">
        <el-card shadow="never">
          <template #header>调班与替班申请</template>
          <el-table :data="data.requests" height="240">
            <el-table-column prop="applicant" label="申请人" />
            <el-table-column prop="replacement" label="替班人" />
            <el-table-column prop="date" label="日期" />
            <el-table-column prop="status" label="状态" />
          </el-table>
        </el-card>
        <el-card shadow="never">
          <template #header>出勤与工时统计</template>
          <el-table :data="data.stats" height="240">
            <el-table-column prop="staffName" label="人员" />
            <el-table-column prop="dayShift" label="白班" />
            <el-table-column prop="middleShift" label="中班" />
            <el-table-column prop="nightShift" label="夜班" />
            <el-table-column prop="restDays" label="休息" />
            <el-table-column prop="overtimeHours" label="加班小时" />
          </el-table>
        </el-card>
      </section>

      <el-card shadow="never">
        <template #header>手动调整记录（一键生成时这些班次锁定保留）</template>
        <el-empty v-if="data.adjustments.length === 0" description="暂无手动调整" :image-size="60" />
        <el-table v-else :data="data.adjustments" height="240">
          <el-table-column prop="adjustedAt" label="调整时间" width="170" />
          <el-table-column prop="staffName" label="人员" />
          <el-table-column prop="date" label="日期" />
          <el-table-column label="班次变更">
            <template #default="{ row }">{{ row.oldShift }} → {{ row.newShift }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" />
        </el-table>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { adjustShift, fetchDashboard, regenerateSchedule } from '../api/schedule';
import ScheduleBoard from '../components/ScheduleBoard.vue';
import { APP_TITLE } from '../constants/app';
import type { DashboardData, ScheduleItem } from '../types/schedule';

const department = ref('急诊科');
const generating = ref(false);
const data = reactive<DashboardData>({ rules: [], schedule: [], conflicts: [], requests: [], stats: [], adjustments: [] });

async function load() {
  Object.assign(data, await fetchDashboard(department.value));
}

async function onAdjust(item: ScheduleItem, shift: string) {
  try {
    Object.assign(data, await adjustShift({ department: department.value, date: item.date, staffName: item.staffName, shift }));
    ElMessage.success(`已将 ${item.date} ${item.staffName} 调整为「${shift}」，冲突与工时统计已更新`);
  } catch {
    ElMessage.error('调整失败，请重试');
  }
}

async function onGenerate() {
  generating.value = true;
  try {
    Object.assign(data, await regenerateSchedule(department.value));
    ElMessage.success('已重新生成排班：手动调整的班次已锁定保留，仅重铺未调整的格子');
  } catch {
    ElMessage.error('生成失败，请重试');
  } finally {
    generating.value = false;
  }
}

onMounted(load);
</script>
