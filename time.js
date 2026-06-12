(function() {
  // 1. 获取 Navigation Timing 数据
  const nav = performance.getEntriesByType('navigation')[0];

  if (!nav) {
    console.warn('⚠️ 当前浏览器不支持 PerformanceNavigationTiming API');
    return;
  }

  // 2. 辅助函数：计算耗时并确保不为负数，保留2位小数
  const getDuration = (start, end) => {
    const duration = end - start;
    return duration >= 0 ? Number(duration.toFixed(2)) : 0;
  };

  // 3. 计算各阶段耗时 (单位: 毫秒)
  const metrics = {
    '🔄 重定向耗时': getDuration(nav.redirectStart, nav.redirectEnd),
    '🔍 DNS 查询耗时': getDuration(nav.domainLookupStart, nav.domainLookupEnd),
    '🔌 TCP 连接耗时': getDuration(nav.connectStart, nav.connectEnd),
    '⏱️ 首字节时间 (TTFB)': getDuration(nav.requestStart, nav.responseStart),
    '📥 内容下载耗时': getDuration(nav.responseStart, nav.responseEnd),
    '🏗️ DOM 解析耗时': getDuration(nav.responseEnd, nav.domInteractive),
    '📜 DOMContentLoaded': getDuration(nav.domContentLoadedEventStart, nav.domContentLoadedEventEnd),
    '✅ DOM 完全加载': getDuration(nav.domInteractive, nav.domComplete),
    '🏁 onload 事件耗时': getDuration(nav.loadEventStart, nav.loadEventEnd),
    '⏳ 页面总加载耗时': getDuration(nav.startTime, nav.loadEventEnd),
    '⬜ 白屏时间 (近似)': getDuration(nav.startTime, nav.responseStart),
    '🖱️ 首次可交互时间': getDuration(nav.startTime, nav.domInteractive)
  };

  // 4. 格式化输出
  console.log('%c🚀 页面性能各阶段耗时统计 (单位: ms)', 'color: #007bff; font-size: 16px; font-weight: bold;');

  // 如果页面还没完全加载，loadEventEnd 会是 0，给出友好提示
  if (nav.loadEventEnd === 0) {
    console.warn('⚠️ 提示：页面尚未完全加载 (load 事件未触发)，"页面总加载耗时" 等指标可能为 0。建议在页面完全加载后再执行此代码。');
  }

  // 以表格形式打印，最直观
  console.table(metrics);
})();