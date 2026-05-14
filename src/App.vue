<template>
  <div class="app-container">
    <div class="header">
      <div style="display: flex; align-items: center; gap: 20px;">
        <h2>📚 Java 编程练习平台</h2>
        <button class="header-btn" @click="enterModule('LOBBY')">🏛️ 返回大厅</button>
        <button class="header-btn" @click="enterModule('ALL')">🌌 上帝视角</button>
      </div>
      <span class="status-tag">Docker 沙箱已连接</span>
    </div>

    <div class="main-layout">
      <div class="sidebar">
        <div class="menu-item" :class="{ active: currentView === 'lobby' || currentView === 'graph' }" @click="enterModule('LOBBY')">
          🌌 知识图谱
        </div>
        <div class="menu-item" :class="{ active: currentView === 'exercise' }" @click="switchView('exercise')">
          📝 练习题库
        </div>
      </div>

      <div class="content-area">

        <div v-show="currentView === 'lobby'" class="lobby-view">
          <h1 class="lobby-title">开启您的 Java 学习之旅</h1>
          <p class="lobby-subtitle">请选择您要修炼的知识模块</p>
          <select v-model="currentModuleName" @change="handleModuleSelect" class="giant-select">
            <option value="LOBBY" disabled>-- 请下拉选择模块 --</option>
            <option value="ALL">🌌 显示全部知识图谱 (上帝视角)</option>
            <option disabled>──────────</option>
            <option v-for="mod in macroModules" :key="mod.id" :value="mod.label">
              📌 {{ mod.label }}
            </option>
          </select>
        </div>

        <div v-show="currentView === 'graph'" class="graph-view" style="display: flex; height: 100%; width: 100%; gap: 20px;">
          <div class="left-graph" :style="{ width: (showEditor || showKnowledgePanel) ? '50%' : '100%' }" style="position: relative;">
            <div class="graph-legend">
              <div class="legend-title">📌 图例</div>
              <div class="legend-item"><span class="shape shape-macro"></span> 模块</div>
              <div class="legend-item"><span class="shape shape-micro"></span> 知识点</div>
              <div class="legend-item"><span class="shape shape-ex-easy"></span> 简单题</div>
              <div class="legend-item"><span class="shape shape-ex-normal"></span> 普通题</div>
              <div class="legend-item"><span class="shape shape-ex-hard"></span> 困难题</div>
              <div class="legend-item"><span class="shape shape-tut"></span> 教程</div>
            </div>
            <div id="container" class="graph-container"></div>
          </div>

          <div class="right-editor" v-if="showEditor">
            <div class="panel-header">
              <h3>📝 {{ currentQuestion.title || '题目详情' }}</h3>
              <span v-if="currentQuestion.difficulty === 'Easy'" class="tag-success">简单</span>
              <span v-else-if="currentQuestion.difficulty === 'Normal'" class="tag-warning">普通</span>
              <span v-else-if="currentQuestion.difficulty === 'Hard'" class="tag-danger">困难</span>
              <button class="close-btn" @click="showEditor = false">✖</button>
            </div>

            <div v-if="showDiagnosis" class="diagnosis-alert">
              <h4 style="margin-top:0"><span style="font-size:20px">🚨</span> 智能诊断报告</h4>
              <p>系统检测到您已连续错误 3 次。系统分析您可能在以下知识点存在盲区：</p>

              <div v-if="testedConcepts.length > 0" class="concept-options">
                <div v-for="(c, index) in testedConcepts" :key="c.id" class="concept-radio" @click="selectWeakPoint(c)">
                  <input type="radio" :checked="selectedWeakPoint && selectedWeakPoint.id === c.id" />
                  <span>{{ c.name }}</span>
                  <span class="weight-tag" :class="{'weight-high': c.weight >= 50}">权重: {{ c.weight }}%</span>
                  <span v-if="index === 0" class="recommend-tag">🔥 最大嫌疑</span>
                </div>
                <div style="margin-top:10px; text-align: right;">
                   <button style="padding: 5px 10px; background: #409EFF; color: #fff; border: none; border-radius: 4px; cursor: pointer;" @click="selectWeakPoint(testedConcepts[0])">让老中医直接开药</button>
                </div>
              </div>

              <div v-if="diagnosisResult.weakPoint" class="prescription-box">
                <h5 style="margin: 10px 0; color: #E6A23C;">📝 老中医处方：</h5>
                <p style="margin: 5px 0; font-size: 14px;">病因锁定：<strong>【{{ diagnosisResult.weakPoint }}】</strong></p>

                <div v-if="diagnosisResult.recommendType === 'Exercise'">
                   <p style="margin: 5px 0; font-size: 14px;">建议退回学习：<strong>{{ diagnosisResult.recommendKnowledge }}</strong></p>
                   <p style="margin: 5px 0; font-size: 14px;">推荐先完成基础练习：
                     <a href="#" class="pulse-link" @click.prevent="openEditorFromList({id: diagnosisResult.recommendExerciseId, customId: diagnosisResult.recommendExerciseCustomId, label: diagnosisResult.recommendExerciseTitle})">
                       {{ diagnosisResult.recommendExerciseTitle }}
                     </a>
                   </p>
                </div>

                <div v-if="diagnosisResult.recommendType === 'Knowledge'" style="background: #e1f3d8; padding: 10px; border-radius: 4px; margin-top:10px;">
                   <p style="margin: 5px 0; color:#67C23A; font-weight:bold;">⚠️ 提醒：该知识点暂无更简单的降级题目！</p>
                   <p style="margin: 5px 0; font-size: 14px;">请直接前往学习知识点百科：</p>
                   <a href="#" class="tutorial-btn" style="background:#67C23A;" @click.prevent="openKnowledgePanel(diagnosisResult.recommendKnowledgeId)">
                     📖 点击前往学习《{{ diagnosisResult.recommendKnowledgeName }}》
                   </a>
                </div>

                <div v-if="diagnosisResult.recommendType === 'Tutorial'" style="background: #fef0f0; padding: 10px; border-radius: 4px; margin-top:10px;">
                   <p style="margin: 5px 0; color:#F56C6C; font-weight:bold;">⚠️ 警告：已触及基础底线！做题毫无意义。</p>
                   <p style="margin: 5px 0; font-size: 14px;">请立即停止盲目猜测，前往观看基础教程：</p>
                   <a :href="diagnosisResult.tutorialUrl" target="_blank" class="tutorial-btn" @click="markTutorialClicked(diagnosisResult.tutorialId)">
                     📺 点击前往学习《{{ diagnosisResult.tutorialTitle }}》
                   </a>
                </div>
              </div>
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
                <span v-for="pre in currentKnowledge.preKnowledges" :key="pre.id" class="k-tag" @click="openKnowledgePanel(pre.id)">
                  {{ pre.name }}
                </span>
              </div>
            </div>

            <div class="relation-section" v-if="currentKnowledge.relatedExercises && currentKnowledge.relatedExercises.length > 0" style="margin-top: 20px;">
              <h4 style="color: #F56C6C; margin-bottom: 8px;">🎯 巩固该知识点的相关练习题：</h4>
              <div class="tag-list">
                <span v-for="ex in currentKnowledge.relatedExercises" :key="ex.id" class="e-tag" @click="openEditor(ex.id, ex.title)">
                  {{ ex.title }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div v-show="currentView === 'exercise'" class="list-view">
          <h3>📝 系统练习题库</h3>
          <div class="card-grid">
            <div class="info-card ex-card" v-for="ex in exercises" :key="ex.id" @click="openEditorFromList(ex)">
              <div class="card-title">{{ ex.label }}</div>
              <div class="card-type">难度: {{ ex.difficulty || 'Normal' }}</div>
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
import { ElMessage } from 'element-plus'

const currentView = ref('lobby')
const macroModules = ref([])
const currentModuleName = ref('LOBBY')
const exercises = ref([])

const showEditor = ref(false)
const currentQuestion = ref({})
const code = ref('')
const isJudging = ref(false)
const judgeResult = ref('')
const isAC = ref(false)

const showKnowledgePanel = ref(false)
const currentKnowledge = ref({})

const waCounts = ref({})
const showDiagnosis = ref(false)
const testedConcepts = ref([])
const selectedWeakPoint = ref(null)
const diagnosisResult = ref({})

let globalNodes = []
let graphInstance = null

const switchView = async (viewName) => {
  currentView.value = viewName
  if (viewName === 'graph' && graphInstance) {
    await nextTick()
    const container = document.getElementById('container')
    if(container) {
      graphInstance.changeSize(container.scrollWidth, container.scrollHeight)
      graphInstance.fitCenter()
    }
  } else if (viewName !== 'graph') {
    showEditor.value = false
    showKnowledgePanel.value = false
  }
}

const fetchLobbyData = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/graph/all?module=LOBBY')
    macroModules.value = response.data.nodes || []
  } catch (e) {
    console.error('加载大厅模块失败')
  }
}

const handleModuleSelect = async () => {
  const moduleName = currentModuleName.value;
  if (moduleName === 'LOBBY') {
    switchView('lobby')
    return;
  }
  enterModule(moduleName)
}

const enterModule = async (moduleName) => {
  currentModuleName.value = moduleName
  if(moduleName === 'LOBBY') {
     switchView('lobby')
     return
  }

  currentView.value = 'graph';
  await nextTick();

  try {
    const response = await axios.get('http://localhost:8080/api/graph/all?module=' + encodeURIComponent(moduleName))
    renderGraph(response.data, moduleName === 'ALL')
  } catch (e) {
    alert("加载图谱失败")
  }
}

const renderGraph = (rawData, isAll = false) => {
  const container = document.getElementById('container')
  if (!container || container.scrollHeight === 0) return
  if (graphInstance) graphInstance.destroy()

  const formattedNodes = []
  const nodeIds = new Set()
  const exs = []

  rawData.nodes.forEach(n => {
    if (n.id && n.label && !nodeIds.has(n.id)) {
      nodeIds.add(n.id)

      if (n.type === 'Exercise') {
        exs.push({ id: n.id, label: n.label, type: n.type, difficulty: n.difficulty, customId: n.exId })
      }

      let color = '#909399'; let shapeType = 'circle'; let radius = 25;

      if (n.type === 'MacroConcept') { shapeType = 'circle'; color = '#E6A23C'; radius = 45; }
      else if (n.type === 'MicroConcept' || n.type === 'KnowledgePoint') { shapeType = 'circle'; color = '#409EFF'; radius = 30; }
      else if (n.type === 'Exercise') {
        shapeType = 'triangle';
        if(n.difficulty === 'Easy') color = '#67C23A'; else if (n.difficulty === 'Hard') color = '#F56C6C'; else color = '#E6A23C';
      } else if (n.type === 'Tutorial') { shapeType = 'rect'; color = '#909399'; }

      const nodeObj = {
        id: String(n.id), type: shapeType,
        data: { label: String(n.label).substring(0, 15), type: n.type, difficulty: n.difficulty, customId: n.exId },
        style: { fill: color, cursor: 'pointer' }
      }

      if (shapeType === 'circle') { nodeObj.style.r = radius }
      else if (shapeType === 'triangle') { nodeObj.style.direction = 'up'; nodeObj.style.size = 35 }
      else { nodeObj.style.width = 100; nodeObj.style.height = 30; nodeObj.style.radius = 4; }

      formattedNodes.push(nodeObj)
    }
  })

  globalNodes = formattedNodes;
  exercises.value = exs;

  const formattedEdges = []
  rawData.edges.forEach(e => {
    if (nodeIds.has(String(e.source)) && nodeIds.has(String(e.target))) {
      let edgeColor = '#Dcdcdc';
      if (e.label.includes('TESTS')) edgeColor = '#F56C6C';
      if (e.label.includes('PRE')) edgeColor = '#409EFF';
      if (e.label.includes('BELONGS')) edgeColor = '#E6A23C';
      if (e.label.includes('EXPL')) edgeColor = '#909399';

      formattedEdges.push({
        source: String(e.source), target: String(e.target),
        data: { label: e.label || '' },
        style: { stroke: edgeColor, lineWidth: e.weight ? (e.weight/20) : 2 }
      })
    }
  })

  const dynamicNodeStrength = isAll ? 2000 : 800;
  const dynamicLinkDistance = isAll ? 250 : 150;

  graphInstance = new Graph({
    container: 'container', width: container.scrollWidth, height: container.scrollHeight,
    fitView: true, fitViewPadding: [40, 40, 40, 40],
    data: { nodes: formattedNodes, edges: formattedEdges },
    node: { style: { labelText: (d) => d.data.label, labelPlacement: 'bottom', labelFontSize: 13, labelFill: '#333' } },
    edge: { style: { labelText: (d) => d.data.label, labelBackground: true, labelBackgroundFill: '#fff', labelFontSize: 10, endArrow: true } },
    layout: { type: 'force', linkDistance: dynamicLinkDistance, preventOverlap: true, nodeStrength: dynamicNodeStrength, collideStrength: 1, alphaDecay: 0.02 },
    behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element']
  })

  graphInstance.render()

  graphInstance.on('node:click', (e) => {
    const nodeId = e.target.id;
    const clickedNode = globalNodes.find(n => n.id === nodeId);
    if (clickedNode) {
      if (clickedNode.data.type === 'Exercise') {
        openEditor(clickedNode.id, clickedNode.data.label, clickedNode.data.customId);
      } else if (clickedNode.data.type === 'MicroConcept' || clickedNode.data.type === 'MacroConcept') {
        openKnowledgePanel(clickedNode.id);
      } else if (clickedNode.data.type === 'Tutorial') {
        const tut = rawData.nodes.find(n => String(n.id) === nodeId);
        if(tut && tut.url) window.open(tut.url, '_blank');
        else ElMessage.info("该教程暂时没有链接");
      }
    }
  })
}

const submitCode = async () => {
  if (!code.value) { ElMessage.warning('代码不能为空！'); return; }
  isJudging.value = true; judgeResult.value = '正在拉取 Docker 沙箱环境并编译代码，请稍候...'; isAC.value = false;

  const qId = currentQuestion.value.customId || currentQuestion.value.id || 4
  try {
    const res = await axios.post('http://localhost:8080/api/judge/submit', { questionId: String(qId), code: code.value })
    judgeResult.value = res.data
    isAC.value = res.data.includes('AC') || res.data.includes('通过')

    if (!isAC.value) {
      waCounts.value[qId] = (waCounts.value[qId] || 0) + 1
      if (waCounts.value[qId] >= 3) {
         await triggerDiagnosis(qId)
      }
    } else {
      waCounts.value[qId] = 0
      showDiagnosis.value = false
    }
  } catch (error) { judgeResult.value = '服务器请求失败: ' + error.message } finally { isJudging.value = false }
}

const triggerDiagnosis = async (qId) => {
  showDiagnosis.value = true
  diagnosisResult.value = {}
  selectedWeakPoint.value = null

  try {
    const res = await axios.get('http://localhost:8080/api/diagnosis/tested-concepts/' + qId)
    testedConcepts.value = res.data || []
  } catch (e) {
    console.error('获取病因列表失败', e)
  }
}

const selectWeakPoint = async (concept) => {
  selectedWeakPoint.value = concept
  try {
    const res = await axios.get("http://localhost:8080/api/diagnosis/recommend?conceptId=" + concept.id + "&conceptName=" + encodeURIComponent(concept.name) + "&currentExerciseId=" + (currentQuestion.value.id || ""))
    diagnosisResult.value = res.data

    if (res.data.recommendType === 'Exercise' && res.data.recommendExerciseId) {
      highlightGraphNode(String(res.data.recommendExerciseId), '#FFD700')
    } else if (res.data.recommendType === 'Knowledge' && res.data.recommendKnowledgeId) {
      highlightGraphNode(String(res.data.recommendKnowledgeId), '#409EFF')
    } else if (res.data.recommendType === 'Tutorial' && res.data.tutorialId) {
      highlightGraphNode(String(res.data.tutorialId), '#F56C6C')
    }
  } catch (e) {
    console.error('开药方失败', e)
  }
}

const markTutorialClicked = (tutId) => {
   ElMessage.success("态度不错！去好好看教程吧，看完再来战！");
   highlightGraphNode(String(tutId), '#67C23A');
}

const openEditor = async (nodeId, nodeLabel, customId) => {
  showKnowledgePanel.value = false; showEditor.value = true; currentQuestion.value = { id: nodeId, title: nodeLabel, customId: customId }; code.value = "正在加载题目详情..."; highlightGraphNode(String(nodeId), '#F56C6C');
  showDiagnosis.value = false;

  const fetchId = customId || nodeId;
  try {
    const res = await axios.get('http://localhost:8080/api/question/' + fetchId)
    if (res.data && res.data.title) { currentQuestion.value = res.data; currentQuestion.value.id = nodeId; currentQuestion.value.customId = customId; code.value = res.data.codeTemplate || 'public class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}' }
    else { code.value = 'public class Main {\n    public static void main(String[] args) {\n        // 数据库中暂无该题目详情\n    }\n}' }
  } catch (e) { code.value = 'public class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}' }
}

const openKnowledgePanel = async (nodeId) => {
  showEditor.value = false; showKnowledgePanel.value = true; currentKnowledge.value = { name: '加载中...' }; highlightGraphNode(String(nodeId), '#409EFF');
  try {
    const res = await axios.get('http://localhost:8080/api/knowledge/detail/' + nodeId);
    if (res.data && res.data.name) currentKnowledge.value = res.data;
    else currentKnowledge.value = { name: '获取信息失败', description: '未能找到该节点的关联信息。' };
  } catch (e) { currentKnowledge.value = { name: '服务器异常', description: '请检查后端服务。' }; }
}

const openEditorFromList = (exNode) => { switchView('graph'); openEditor(exNode.id, exNode.label || exNode.title, exNode.customId); }

const highlightGraphNode = (nodeId, highlightColor) => {
  if (!graphInstance) return
  switchView('graph')
  try {
    const nodesData = graphInstance.getNodeData()
    const updatedNodes = nodesData.map(n => {
      if (String(n.id) === String(nodeId)) return { id: n.id, style: { ...n.style, stroke: highlightColor, lineWidth: 6, shadowColor: highlightColor, shadowBlur: 20 } }
      else return { id: n.id, style: { ...n.style, stroke: 'none', lineWidth: 0, shadowColor: 'transparent', shadowBlur: 0 } }
    })
    graphInstance.updateNodeData(updatedNodes); graphInstance.draw()
  } catch (e) {}
}

onMounted(() => { fetchLobbyData() })
</script>

<style>
body { margin: 0; background-color: #f5f7fa; font-family: sans-serif; }
.app-container { height: 100vh; display: flex; flex-direction: column; overflow: hidden; }
.header { background-color: #fff; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); padding: 0 20px; display: flex; justify-content: space-between; align-items: center; z-index: 10; height: 60px; flex-shrink: 0; }
.header-btn { padding: 8px 16px; font-size: 14px; background-color: #f5f7fa; border: 1px solid #dcdfe6; border-radius: 6px; cursor: pointer; color: #606266; font-weight: bold; transition: all 0.3s; margin-left: 10px;}
.header-btn:hover { border-color: #409EFF; color: #409EFF; background-color: #ecf5ff; }
.status-tag { background-color: #67C23A; color: #fff; padding: 5px 10px; border-radius: 4px; font-size: 14px; }

.main-layout { display: flex; height: calc(100vh - 60px); }
.sidebar { width: 160px; background-color: #2c3e50; color: #bfcbd9; display: flex; flex-direction: column; padding-top: 20px; flex-shrink: 0; }
.menu-item { padding: 15px 20px; cursor: pointer; font-size: 14px; transition: all 0.3s; border-left: 4px solid transparent; }
.menu-item:hover { background-color: #1f2d3d; color: #fff; }
.menu-item.active { background-color: #1f2d3d; color: #409EFF; border-left: 4px solid #409EFF; font-weight: bold; }
.content-area { flex-grow: 1; padding: 20px; overflow: hidden; background-color: #f5f7fa; }

.lobby-view { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%); border-radius: 12px; }
.lobby-title { font-size: 36px; color: #303133; margin-bottom: 10px; letter-spacing: 2px;}
.lobby-subtitle { font-size: 18px; color: #909399; margin-bottom: 40px; }
.giant-select { padding: 15px 30px; font-size: 20px; border-radius: 8px; border: 2px solid #409EFF; color: #303133; outline: none; background-color: #fff; cursor: pointer; font-weight: bold; width: 450px; box-shadow: 0 4px 15px rgba(64,158,255,0.2); transition: all 0.3s; }

.graph-legend { position: absolute; top: 10px; right: 10px; background: rgba(255, 255, 255, 0.7); padding: 5px 8px; border-radius: 4px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); border: 1px solid #ebeef5; z-index: 100; pointer-events: none; backdrop-filter: blur(2px); }
.legend-title { font-weight: bold; margin-bottom: 4px; color: #606266; font-size: 11px; border-bottom: 1px solid #ebeef5; padding-bottom: 3px; }
.legend-item { display: flex; align-items: center; margin-bottom: 3px; font-size: 10px; color: #909399; font-weight: normal;}
.shape { display: inline-block; margin-right: 6px; }
.shape-macro { width: 12px; height: 12px; border-radius: 50%; background: #E6A23C; }
.shape-micro { width: 10px; height: 10px; border-radius: 50%; background: #409EFF; }
.shape-ex-easy { width: 0; height: 0; border-left: 6px solid transparent; border-right: 6px solid transparent; border-bottom: 10px solid #67C23A; }
.shape-ex-normal { width: 0; height: 0; border-left: 6px solid transparent; border-right: 6px solid transparent; border-bottom: 10px solid #E6A23C; }
.shape-ex-hard { width: 0; height: 0; border-left: 6px solid transparent; border-right: 6px solid transparent; border-bottom: 10px solid #F56C6C; }
.shape-tut { width: 10px; height: 8px; background: #909399; border-radius: 2px; }

.left-graph { transition: width 0.3s ease; height: 100%; background-color: #fff; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); overflow: hidden; position: relative;}
.graph-container { width: 100%; height: 100%; }
.right-editor { width: 50%; height: 100%; background-color: #fff; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); display: flex; flex-direction: column; padding: 20px; box-sizing: border-box; animation: slideIn 0.3s ease; overflow-y: auto;}
@keyframes slideIn { from { transform: translateX(50px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
.panel-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #eee; padding-bottom: 10px; margin-bottom: 15px; }
.close-btn { background: none; border: none; font-size: 18px; cursor: pointer; color: #999; }

.diagnosis-alert { background-color: #fdf6ec; border: 1px solid #faecd8; padding: 15px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 12px 0 rgba(230,162,60,0.1);}
.concept-options { background: #fff; padding: 10px; border-radius: 6px; border: 1px dashed #e4e7ed; margin-bottom: 10px;}
.concept-radio { padding: 8px; border-radius: 4px; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: background 0.2s;}
.concept-radio:hover { background: #f5f7fa; }
.weight-tag { font-size: 12px; background: #f4f4f5; color: #909399; padding: 2px 6px; border-radius: 4px; margin-left: auto; }
.weight-high { background: #fef0f0; color: #f56c6c; font-weight: bold; }
.recommend-tag { font-size: 12px; color: #f56c6c; font-weight: bold;}
.prescription-box { background: #f0f9eb; padding: 12px; border-radius: 6px; border-left: 4px solid #67C23A; animation: slideIn 0.3s ease;}
.pulse-link { color: #409EFF; font-weight: bold; text-decoration: none; display: inline-block; animation: linkPulse 2s infinite;}
.tutorial-btn { display: inline-block; background: #F56C6C; color: white; padding: 8px 15px; border-radius: 4px; text-decoration: none; font-weight: bold; margin-top: 5px; transition: transform 0.2s;}
.tutorial-btn:hover { transform: scale(1.05); }
@keyframes linkPulse { 0% { opacity: 1; } 50% { opacity: 0.6; color: #F56C6C; } 100% { opacity: 1; } }

.knowledge-desc { background-color: #ecf5ff; padding: 15px; border-radius: 6px; color: #409EFF; margin-bottom: 20px; font-size: 15px; line-height: 1.6; border-left: 4px solid #409EFF;}
.relation-section { margin-bottom: 20px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 10px; }
.k-tag { background-color: #f0f9eb; color: #67C23A; border: 1px solid #e1f3d8; padding: 6px 12px; border-radius: 20px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.k-tag:hover { background-color: #67C23A; color: white; transform: scale(1.05); }
.e-tag { background-color: #fef0f0; color: #F56C6C; border: 1px solid #fde2e2; padding: 6px 12px; border-radius: 4px; font-size: 13px; cursor: pointer; font-weight: bold; transition: all 0.2s; }
.e-tag:hover { background-color: #F56C6C; color: white; transform: scale(1.05); }

.question-desc { background-color: #f4f4f5; padding: 15px; border-radius: 4px; color: #666; margin-bottom: 20px; font-size: 14px; }
.custom-editor { width: 100%; background-color: #1e1e1e; color: #d4d4d4; font-family: Consolas, monospace; font-size: 14px; border: 1px solid #333; border-radius: 4px; padding: 10px; box-sizing: border-box; resize: none; }
.action-bar { margin-top: 20px; text-align: right; }
.submit-btn { width: 100%; font-weight: bold; padding: 12px; background-color: #409EFF; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
.submit-btn:disabled { background-color: #a0cfff; cursor: not-allowed; }
.result-area { margin-top: 20px; background-color: #f8f9fa; border-radius: 4px; padding: 15px; border: 1px solid #e4e7ed; overflow-x: auto; }
.ac-result { color: #67C23A; font-weight: bold; }
.wa-result { color: #F56C6C; }

.list-view { height: 100%; overflow-y: auto; padding-right: 10px; }
.list-view h3 { color: #303133; margin-top: 0; padding-bottom: 10px; border-bottom: 2px solid #ebeef5; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; margin-top: 20px; }
.info-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05); transition: transform 0.2s; border-top: 4px solid #ddd; cursor: pointer; }
.info-card:hover { transform: translateY(-5px); box-shadow: 0 4px 16px 0 rgba(0,0,0,0.1); }
.ex-card { border-top-color: #F56C6C; display: flex; flex-direction: column; justify-content: space-between;}
.card-title { font-size: 16px; font-weight: bold; color: #333; margin-bottom: 10px; line-height: 1.4; }
.start-btn { margin-top: 15px; padding: 8px; background-color: #F56C6C; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
</style>