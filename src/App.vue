<template>
  <div class="app-container">
    <div class="header">
      <h2>📚 基于知识图谱的 Java 编程练习平台</h2>
      <span class="status-tag">Docker 沙箱已连接</span>
    </div>
    
    <div class="main-layout">
      <div class="sidebar">
        <div class="menu-item" :class="{ active: currentView === 'graph' }" @click="switchView('graph')">
          🌌 知识图谱
        </div>
        <div class="menu-item" :class="{ active: currentView === 'knowledge' }" @click="switchView('knowledge')">
          📖 知识点列表
        </div>
        <div class="menu-item" :class="{ active: currentView === 'exercise' }" @click="switchView('exercise')">
          📝 练习题列表
        </div>
      </div>

      <div class="content-area">
        <div v-show="currentView === 'graph'" class="graph-view" style="display: flex; height: 100%; width: 100%; gap: 20px;">
          <div class="left-graph" :style="{ width: (showEditor || showKnowledgePanel) ? '50%' : '100%' }">
            <div id="container" class="graph-container"></div>
          </div>

          <div class="right-editor" v-if="showEditor">
            <div class="panel-header">
              <h3>📝 {{ currentQuestion.title || '题目详情' }}</h3>
              <button class="close-btn" @click="showEditor = false">✖</button>
            </div>
            
            <div v-if="showDiagnosis" class="diagnosis-alert">
              <h4>🚨 智能诊断报告</h4>
              <p>诊断结果：<strong>【{{ diagnosisResult.weakPoint }}】</strong> 掌握不牢固。</p>
              <p>建议回退学习：<strong>{{ diagnosisResult.recommendKnowledge }}</strong></p>
              <p>推荐先完成基础练习：<a href="#" @click.prevent="openEditorFromList({id: diagnosisResult.recommendExerciseId, label: diagnosisResult.recommendExerciseTitle})">{{ diagnosisResult.recommendExerciseTitle }}</a></p>
            </div>

            <div class="question-desc">
              <p style="white-space: pre-wrap;"><strong>题目描述：</strong> {{ currentQuestion.content || '请根据面向对象知识，编写满足要求的 Java 代码。' }}</p>
            </div>
            <div class="code-area">
              <textarea v-model="code" rows="12" class="custom-editor"></textarea>
            </div>
            <div class="action-bar">
              <button @click="submitCode" :disabled="isJudging" class="submit-btn">
                {{ isJudging ? '⏳ 正在判题中...' : '🚀 提交运行 (Docker 沙箱)' }}
              </button>
            </div>
            <div class="result-area" v-if="judgeResult">
              <h4>🖥️ 运行结果：</h4>
              <pre :class="{'ac-result': isAC, 'wa-result': !isAC}">{{ judgeResult }}</pre>
            </div>
          </div>

          <div class="right-editor knowledge-panel" v-if="showKnowledgePanel">
            <div class="panel-header" style="border-bottom-color: #409EFF;">
              <h3 style="color: #409EFF;">📖 {{ currentKnowledge.name || '知识点加载中...' }}</h3>
              <button class="close-btn" @click="showKnowledgePanel = false">✖</button>
            </div>
            
            <div class="knowledge-desc">
              <p style="white-space: pre-wrap;">{{ currentKnowledge.description || '加载中，请稍候...' }}</p>
            </div>

            <div class="relation-section" v-if="currentKnowledge.preKnowledges && currentKnowledge.preKnowledges.length > 0">
              <h4 style="color: #67C23A; margin-bottom: 8px;">🔄 建议先掌握以下前置知识：</h4>
              <div class="tag-list">
                <span 
                  v-for="pre in currentKnowledge.preKnowledges" 
                  :key="pre.id" 
                  class="k-tag"
                  @click="openKnowledgePanel(pre.id)"
                >
                  {{ pre.name }}
                </span>
              </div>
            </div>

            <div class="relation-section" v-if="currentKnowledge.relatedExercises && currentKnowledge.relatedExercises.length > 0" style="margin-top: 20px;">
              <h4 style="color: #F56C6C; margin-bottom: 8px;">🎯 巩固该知识点的相关练习题：</h4>
              <div class="tag-list">
                <span 
                  v-for="ex in currentKnowledge.relatedExercises" 
                  :key="ex.id" 
                  class="e-tag"
                  @click="openEditor(ex.id, ex.title)"
                >
                  {{ ex.title }}
                </span>
              </div>
            </div>
          </div>

        </div>

        <div v-show="currentView === 'knowledge'" class="list-view">
          <h3>📖 系统涵盖的知识点</h3>
          <div class="card-grid">
            <div class="info-card kp-card" v-for="kp in knowledgePoints" :key="kp.id" @click="openKnowledgeFromList(kp)">
              <div class="card-title">{{ kp.label }}</div>
              <div class="card-type">类型: {{ kp.type === 'Concept' ? '宏观概念' : '具体知识点' }}</div>
              <button class="start-btn" style="background-color: #409EFF; margin-top: 10px;">探索详情</button>
            </div>
          </div>
        </div>

        <div v-show="currentView === 'exercise'" class="list-view">
          <h3>📝 系统练习题库</h3>
          <div class="card-grid">
            <div class="info-card ex-card" v-for="ex in exercises" :key="ex.id" @click="openEditorFromList(ex)">
              <div class="card-title">{{ ex.label }}</div>
              <button class="start-btn">开始挑战</button>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, nextTick } from 'vue'
import { Graph } from '@antv/g6'
import axios from 'axios'

const currentView = ref('graph')
const knowledgePoints = ref([])
const exercises = ref([])

const showEditor = ref(false)
const currentQuestion = ref({})
const code = ref('')
const isJudging = ref(false)
const judgeResult = ref('')
const isAC = ref(false)

const waCounts = ref({})
const showDiagnosis = ref(false)
const diagnosisResult = ref({})

const showKnowledgePanel = ref(false)
const currentKnowledge = ref({})

let globalNodes = []
let graphInstance = null

const switchView = async (viewName) => {
  currentView.value = viewName
  if (viewName === 'graph' && graphInstance) {
    await nextTick()
    graphInstance.fitView()
  } else if (viewName !== 'graph') {
    showEditor.value = false
    showKnowledgePanel.value = false
  }
}

const submitCode = async () => {
  if (!code.value) {
    alert('代码不能为空！')
    return
  }
  isJudging.value = true
  judgeResult.value = '正在拉取 Docker 沙箱环境并编译代码，请稍候...'
  isAC.value = false
  showDiagnosis.value = false
  
  const qId = currentQuestion.value.id || 4
  try {
    const res = await axios.post('http://localhost:8080/api/judge/submit', {
      questionId: qId,
      code: code.value
    })
    judgeResult.value = res.data
    isAC.value = res.data.includes('AC') || res.data.includes('通过')
    
    if (!isAC.value) {
      waCounts.value[qId] = (waCounts.value[qId] || 0) + 1
      if (waCounts.value[qId] >= 3) {
        await fetchDiagnosis(qId)
      }
    } else {
      waCounts.value[qId] = 0
    }
  } catch (error) {
    judgeResult.value = '服务器请求失败: ' + error.message
  } finally {
    isJudging.value = false
  }
}

const fetchDiagnosis = async (qId) => {
  try {
    const res = await axios.get('http://localhost:8080/api/diagnosis/analyze/' + qId)
    if (res.data && res.data.weakPoint) {
      diagnosisResult.value = res.data
      showDiagnosis.value = true
      highlightGraphNode(String(res.data.recommendExerciseId), '#FFD700')
    }
  } catch (e) {
    console.error('获取诊断失败', e)
  }
}

const highlightGraphNode = (nodeId, highlightColor) => {
  if (!graphInstance) return
  switchView('graph')
  
  try {
    const nodesData = graphInstance.getNodeData()
    const updatedNodes = nodesData.map(n => {
      if (String(n.id) === String(nodeId)) {
        return { 
          id: n.id, 
          style: { ...n.style, stroke: highlightColor, lineWidth: 6, shadowColor: highlightColor, shadowBlur: 20 } 
        }
      } else {
        return { 
          id: n.id, 
          style: { ...n.style, stroke: 'none', lineWidth: 0, shadowColor: 'transparent', shadowBlur: 0 } 
        }
      }
    })
    
    graphInstance.updateNodeData(updatedNodes)
    graphInstance.draw()
  } catch (e) {
    console.error("高亮特效失败 (可能与 V5 API 兼容有关):", e)
  }
}

const openEditor = async (nodeId, nodeLabel) => {
  showKnowledgePanel.value = false 
  showEditor.value = true
  judgeResult.value = ''
  showDiagnosis.value = false
  currentQuestion.value = { id: nodeId, title: nodeLabel }
  code.value = '正在加载题目详情...'
  
  highlightGraphNode(String(nodeId), '#F56C6C')

  try {
    const res = await axios.get('http://localhost:8080/api/question/' + nodeId)
    if (res.data && res.data.title) {
      currentQuestion.value = res.data
      code.value = res.data.codeTemplate || 'public class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}'
    } else {
      code.value = 'public class Main {\n    public static void main(String[] args) {\n        // 数据库中暂无该题目详情\n    }\n}'
    }
  } catch (e) {
    code.value = 'public class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}'
  }
}

const openKnowledgePanel = async (nodeId) => {
  showEditor.value = false 
  showKnowledgePanel.value = true
  currentKnowledge.value = { name: '加载中...' }
  
  highlightGraphNode(String(nodeId), '#409EFF')

  try {
    const res = await axios.get('http://localhost:8080/api/knowledge/detail/' + nodeId)
    if (res.data && res.data.name) {
      currentKnowledge.value = res.data
    } else {
      currentKnowledge.value = { name: '获取信息失败', description: '未能找到该节点的关联信息。' }
    }
  } catch (e) {
    console.error('获取知识详情失败', e)
    currentKnowledge.value = { name: '服务器异常', description: '请检查后端服务是否启动。' }
  }
}

const openEditorFromList = (exNode) => {
  switchView('graph')
  openEditor(exNode.id, exNode.label || exNode.title)
}

const openKnowledgeFromList = (kpNode) => {
  switchView('graph')
  openKnowledgePanel(kpNode.id)
}

onMounted(async () => {
  const container = document.getElementById('container')
  if (!container) return
  
  let rawData = { nodes: [], edges: [] }
  try {
    const response = await axios.get('http://localhost:8080/api/graph/all')
    rawData = response.data
  } catch (error) {
    console.error("获取图谱数据失败")
    return
  }

  const formattedNodes = []
  const nodeIds = new Set() 
  const kps = []
  const exs = []
  
  rawData.nodes.forEach(n => {
    if (n.id && n.label && !nodeIds.has(n.id)) {
      nodeIds.add(n.id)
      
      if (n.type === 'KnowledgePoint' || n.type === 'Concept' || n.type === 'AbilityTarget') {
        kps.push({ id: n.id, label: n.label, type: n.type })
      } else if (n.type === 'Exercise') {
        exs.push({ id: n.id, label: n.label, type: n.type })
      }
      
      let color = '#909399'; let shapeType = 'circle'; let radius = 25;
      
      if (n.type === 'AbilityTarget') { color = '#E6A23C'; radius = 40; } 
      else if (n.type === 'Concept') { color = '#409EFF'; radius = 35; }
      else if (n.type === 'KnowledgePoint') { color = '#67C23A'; radius = 25; }
      else if (n.type === 'Exercise') { color = '#F56C6C'; shapeType = 'rect'; radius = 5; }
      
      const nodeObj = {
        id: String(n.id),
        type: shapeType,
        data: { label: String(n.label).substring(0, 15), type: n.type },
        style: { fill: color, cursor: 'pointer' } 
      }
      
      if (shapeType === 'circle') { nodeObj.style.r = radius } 
      else { nodeObj.style.width = 120; nodeObj.style.height = 40; nodeObj.style.radius = 4; }
      formattedNodes.push(nodeObj)
    }
  })
  
  globalNodes = formattedNodes;
  knowledgePoints.value = kps;
  exercises.value = exs;

  const formattedEdges = []
  rawData.edges.forEach(e => {
    if (nodeIds.has(String(e.source)) && nodeIds.has(String(e.target))) {
      formattedEdges.push({ source: String(e.source), target: String(e.target), data: { label: e.label || '' } })
    }
  })
  
  graphInstance = new Graph({
    container: 'container',
    width: container.scrollWidth || 1000,
    height: container.scrollHeight || 600,
    autoFit: 'view',
    data: { nodes: formattedNodes, edges: formattedEdges },
    node: { style: { labelText: (d) => d.data.label, labelPlacement: 'bottom', labelFontSize: 12, labelFill: '#333' } },
    edge: { style: { labelText: (d) => d.data.label, labelBackground: true, labelBackgroundFill: '#fff', labelFontSize: 10, endArrow: true } },
    layout: { type: 'force', linkDistance: 120, preventOverlap: true, nodeStrength: 1000, collideStrength: 1 },
    behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element']
  })

  graphInstance.render()

  graphInstance.on('node:click', (e) => {
    const nodeId = e.target.id;
    const clickedNode = globalNodes.find(n => n.id === nodeId);
    if (clickedNode) {
      if (clickedNode.data.type === 'Exercise') {
        openEditor(clickedNode.id, clickedNode.data.label);
      } else {
        openKnowledgePanel(clickedNode.id);
      }
    }
  })
})
</script>

<style>
body { margin: 0; background-color: #f5f7fa; font-family: sans-serif; }
.app-container { height: 100vh; display: flex; flex-direction: column; overflow: hidden; }
.header { background-color: #fff; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); padding: 0 20px; display: flex; justify-content: space-between; align-items: center; z-index: 10; height: 60px; flex-shrink: 0; }
.status-tag { background-color: #67C23A; color: #fff; padding: 5px 10px; border-radius: 4px; font-size: 14px; }
.main-layout { display: flex; height: calc(100vh - 60px); }
.sidebar { width: 200px; background-color: #2c3e50; color: #bfcbd9; display: flex; flex-direction: column; padding-top: 20px; flex-shrink: 0; }
.menu-item { padding: 15px 20px; cursor: pointer; font-size: 16px; transition: all 0.3s; border-left: 4px solid transparent; }
.menu-item:hover { background-color: #1f2d3d; color: #fff; }
.menu-item.active { background-color: #1f2d3d; color: #409EFF; border-left: 4px solid #409EFF; font-weight: bold; }
.content-area { flex-grow: 1; padding: 20px; overflow: hidden; background-color: #f5f7fa; }
.list-view { height: 100%; overflow-y: auto; padding-right: 10px; }
.list-view h3 { color: #303133; margin-top: 0; padding-bottom: 10px; border-bottom: 2px solid #ebeef5; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; margin-top: 20px; }
.info-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05); transition: transform 0.2s; border-top: 4px solid #ddd; cursor: pointer; }
.info-card:hover { transform: translateY(-5px); box-shadow: 0 4px 16px 0 rgba(0,0,0,0.1); }
.kp-card { border-top-color: #409EFF; }
.ex-card { border-top-color: #F56C6C; display: flex; flex-direction: column; justify-content: space-between;}
.card-title { font-size: 16px; font-weight: bold; color: #333; margin-bottom: 10px; line-height: 1.4; }
.card-type { font-size: 13px; color: #909399; }
.start-btn { margin-top: 15px; padding: 8px; background-color: #F56C6C; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
.start-btn:hover { opacity: 0.8; }
.left-graph { transition: width 0.3s ease; height: 100%; background-color: #fff; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); overflow: hidden; }
.graph-container { width: 100%; height: 100%; }

/* 右侧面板通用样式 */
.right-editor { width: 50%; height: 100%; background-color: #fff; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); display: flex; flex-direction: column; padding: 20px; box-sizing: border-box; animation: slideIn 0.3s ease; overflow-y: auto;}
@keyframes slideIn { from { transform: translateX(50px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
.panel-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #eee; padding-bottom: 10px; margin-bottom: 15px; }
.close-btn { background: none; border: none; font-size: 18px; cursor: pointer; color: #999; }

/* 知识探索面板专属样式 */
.knowledge-desc { background-color: #ecf5ff; padding: 15px; border-radius: 6px; color: #409EFF; margin-bottom: 20px; font-size: 15px; line-height: 1.6; border-left: 4px solid #409EFF;}
.relation-section { margin-bottom: 20px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 10px; }
.k-tag { background-color: #f0f9eb; color: #67C23A; border: 1px solid #e1f3d8; padding: 6px 12px; border-radius: 20px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.k-tag:hover { background-color: #67C23A; color: white; transform: scale(1.05); }
.e-tag { background-color: #fef0f0; color: #F56C6C; border: 1px solid #fde2e2; padding: 6px 12px; border-radius: 4px; font-size: 13px; cursor: pointer; font-weight: bold; transition: all 0.2s; }
.e-tag:hover { background-color: #F56C6C; color: white; transform: scale(1.05); }

/* 做题面板专属样式 */
.diagnosis-alert { background-color: #fdf6ec; border: 1px solid #faecd8; padding: 15px; border-radius: 8px; margin-bottom: 20px; animation: pulse 2s infinite; }
.diagnosis-alert h4 { color: #E6A23C; margin-top: 0; margin-bottom: 10px; }
.diagnosis-alert a { color: #409EFF; font-weight: bold; text-decoration: none; cursor: pointer; }
.diagnosis-alert a:hover { text-decoration: underline; }
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.4); } 70% { box-shadow: 0 0 0 10px rgba(230, 162, 60, 0); } 100% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0); } }
.question-desc { background-color: #f4f4f5; padding: 15px; border-radius: 4px; color: #666; margin-bottom: 20px; font-size: 14px; }
.custom-editor { width: 100%; background-color: #1e1e1e; color: #d4d4d4; font-family: Consolas, monospace; font-size: 14px; border: 1px solid #333; border-radius: 4px; padding: 10px; box-sizing: border-box; resize: none; }
.action-bar { margin-top: 20px; text-align: right; }
.submit-btn { width: 100%; font-weight: bold; padding: 12px; background-color: #409EFF; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
.submit-btn:disabled { background-color: #a0cfff; cursor: not-allowed; }
.result-area { margin-top: 20px; background-color: #f8f9fa; border-radius: 4px; padding: 15px; border: 1px solid #e4e7ed; overflow-x: auto; }
.ac-result { color: #67C23A; font-weight: bold; }
.wa-result { color: #F56C6C; }
</style>