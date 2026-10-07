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
      <el-alert title="规则引擎已加载：连续工作上限、周末轮循、夜班后禁接白班、节假日优先级。" type="info" show-icon />

      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>排班规则</span>
          </div>
        </template>
        <el-tag v-for="rule in data.rules" :key="rule" class="tag">{{ rule }}</el-tag>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>冲突检测</span>
            <el-tag v-if="!data.conflicts.length" type="success" size="small">暂无冲突</el-tag>
          </div>
        </template>
        <el-empty v-if="!data.conflicts.length" description="无夜班接白班、无连续上班超 5 天" :image-size="60" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="alert in data.conflicts"
            :key="`${alert.staffName}-${alert.date}-${alert.message}`"
            :type="alert.level === 'danger' ? 'danger' : 'warning'"
          >
            {{ alert.date }} {{ alert.staffName }}：{{ alert.message }}
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>可视化排班表（点击任意格子直接换班）</span>
            <el-button type="primary" :loading="generating" @click="regenerate">一键生成</el-button>
          </div>
        </template>
        <el-alert
          :title="`一键生成规则：${data.regeneratePolicy}`"
          type="success"
          :closable="false"
          show-icon
          class="policy-alert"
        />
        <ScheduleBoard :items="data.schedule" :department="department" @updated="applyData" />
      </el-card>

      <section class="grid grid-3">
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
          <template #header>
            <div class="card-head">
              <span>手动调整记录</span>
              <el-tag type="warning" size="small">留痕 · 一键生成照此保留</el-tag>
            </div>
          </template>
          <el-empty v-if="!data.adjustments.length" description="暂无手动调整" :image-size="60" />
          <el-timeline v-else class="adjust-list">
            <el-timeline-item
              v-for="record in data.adjustments"
              :key="record.id"
              :timestamp="`${record.createdAt} · ${record.operator}`"
              type="primary"
            >
              {{ record.date }} {{ record.staffName }}：{{ record.oldShift }} →
              <strong>{{ record.newShift }}</strong>
              <small v-if="record.reason">（{{ record.reason }}）</small>
            </el-timeline-item>
          </el-timeline>
        </el-card>
        <el-card shadow="never">
          <template #header>出勤与工时统计（随调整实时重算）</template>
          <el-table :data="data.stats" height="240">
            <el-table-column prop="staffName" label="人员" width="78" />
            <el-table-column prop="dayShift" label="白班" width="52" />
            <el-table-column prop="middleShift" label="中班" width="52" />
            <el-table-column prop="nightShift" label="夜班" width="52" />
            <el-table-column prop="workDays" label="出勤" width="52" />
            <el-table-column prop="overtimeHours" label="加班h" width="60" />
          </el-table>
        </el-card>
      </section>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchDashboard, regenerateSchedule } from '../api/schedule';
import ScheduleBoard from '../components/ScheduleBoard.vue';
import { APP_TITLE } from '../constants/app';
import type { DashboardData } from '../types/schedule';

const department = ref('急诊科');
const generating = ref(false);
const data = reactive<DashboardData>({
  rules: [],
  regeneratePolicy: '',
  schedule: [],
  conflicts: [],
  requests: [],
  adjustments: [],
  stats: [],
});

function applyData(next: DashboardData) {
  Object.assign(data, next);
}

async function load() {
  applyData(await fetchDashboard(department.value));
}

async function regenerate() {
  generating.value = true;
  try {
    applyData(await regenerateSchedule(department.value));
    ElMessage.success('已重铺未调整格子，手动改过的格子按调整记录保留');
  } catch {
    ElMessage.error('一键生成失败，请稍后重试');
  } finally {
    generating.value = false;
  }
}

onMounted(load);
</script>
