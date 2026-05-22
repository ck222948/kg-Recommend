<template>
  <div class="app-container">
    <div v-if="!currentUser" class="auth-page">
      <div class="auth-stage">
        <div class="auth-visual">
          <div class="visual-title">
            <h2>Java 编程练习平台</h2>
            <p>知识图谱驱动的学习与内容管理系统</p>
          </div>
          <div class="door-scene">
            <div class="door-light"></div>
            <div class="door">
              <span class="door-panel top-left"></span>
              <span class="door-panel top-right"></span>
              <span class="door-panel bottom-left"></span>
              <span class="door-panel bottom-right"></span>
              <span class="door-knob"></span>
            </div>
            <div class="door-step"></div>
          </div>
        </div>

        <div class="auth-main">
          <div class="auth-panel">
            <div class="auth-card-head">
              <span>{{ authMode === 'login' ? 'ALREADY MEMBERS' : 'CREATE ACCOUNT' }}</span>
              <small>{{ authMode === 'login' ? 'Need help?' : 'Welcome' }}</small>
            </div>
            <form class="auth-form" autocomplete="off" @submit.prevent="submitAuth">
              <input v-model="authForm.username" name="kg_oj_account" autocomplete="off" placeholder="请输入账号" />
              <input v-if="authMode === 'register'" v-model="authForm.phone" name="kg_oj_phone" autocomplete="off" placeholder="请输入手机号" />
              <input v-model="authForm.password" name="kg_oj_password" type="password" autocomplete="new-password" placeholder="请输入密码" />
              <input v-if="authMode === 'register'" v-model="authForm.confirmPassword" name="kg_oj_confirm_password" type="password" autocomplete="new-password" placeholder="请确认密码" />
              <div class="role-switch">
                <label :class="{ active: authForm.role === 'STUDENT' }">
                  <input v-model="authForm.role" type="radio" value="STUDENT" />
                  <span></span>
                  用户
                </label>
                <label :class="{ active: authForm.role === 'ADMIN' }">
                  <input v-model="authForm.role" type="radio" value="ADMIN" />
                  <span></span>
                  管理员
                </label>
              </div>
              <button type="submit" class="auth-submit">{{ authMode === 'login' ? '登录' : '注册' }}</button>
            </form>
          </div>
          <button class="auth-link-btn" @click="authMode = authMode === 'login' ? 'register' : 'login'">
            {{ authMode === 'login' ? '没有账号，注册一个' : '已有账号，去登录' }}
          </button>
        </div>
      </div>
    </div>

    <div v-else-if="currentUser.role === 'ADMIN'" class="admin-shell">
      <div class="admin-header">
        <div>
          <h2>内容管理后台</h2>
          <span>管理员：{{ currentUser.username }}</span>
        </div>
        <button class="logout-btn" @click="logout">退出登录</button>
      </div>

      <div class="admin-layout">
        <div class="admin-sidebar">
          <button :class="{ active: adminView === 'knowledge' }" @click="adminView = 'knowledge'">录入知识点</button>
          <button :class="{ active: adminView === 'question' }" @click="adminView = 'question'">录入题目</button>
          <button :class="{ active: adminView === 'resource' }" @click="adminView = 'resource'">录入资源</button>
          <button :class="{ active: adminView === 'module' }" @click="adminView = 'module'">录入模块</button>
          <button :class="{ active: adminView === 'user' }" @click="adminView = 'user'">用户管理</button>
        </div>

        <div class="admin-content">
          <div v-show="adminView === 'knowledge'" class="admin-form-card">
            <h3>{{ selectedKnowledgeId ? '修改知识点' : '录入知识点' }}</h3>
            <div class="admin-manage-row">
              <select v-model.number="selectedKnowledgeId" @change="loadSelectedKnowledge">
                <option :value="null">新增知识点</option>
                <option v-for="kp in adminOptions.knowledges" :key="kp.nodeId" :value="kp.detailId">
                  {{ kp.detailId || '未绑定ID' }} - {{ kp.name }}
                </option>
              </select>
              <button class="admin-secondary-btn" @click="resetKnowledgeForm">新增</button>
              <button class="admin-danger-btn" :disabled="!selectedKnowledgeId" @click="deleteKnowledge">删除</button>
            </div>
            <div class="form-grid">
              <label>知识点 ID</label>
              <input v-model.number="knowledgeForm.id" type="number" :disabled="!!selectedKnowledgeId" placeholder="对应 MySQL knowledge_detail.id" />
              <label>知识点名称</label>
              <input v-model="knowledgeForm.name" placeholder="例如：方法重写（override）" />
              <label>所属模块</label>
              <select v-model="knowledgeForm.moduleName">
                <option value="">暂不选择</option>
                <option v-for="mod in adminOptions.modules" :key="mod.nodeId" :value="mod.name">{{ mod.name }}</option>
              </select>
              <label>前置知识点</label>
              <select v-model.number="knowledgeForm.preKnowledgeId">
                <option :value="null">无</option>
                <option v-for="kp in adminOptions.knowledges" :key="kp.nodeId" :value="kp.detailId">{{ kp.name }}</option>
              </select>
              <label>视频/资料链接</label>
              <input v-model="knowledgeForm.videoUrl" placeholder="可选" />
              <label>详细内容</label>
              <textarea v-model="knowledgeForm.description" rows="8" placeholder="请输入知识点详细说明"></textarea>
            </div>
            <button class="admin-primary-btn" @click="saveKnowledge">{{ selectedKnowledgeId ? '保存修改' : '保存知识点' }}</button>
          </div>

          <div v-show="adminView === 'question'" class="admin-form-card">
            <h3>{{ selectedQuestionId ? '修改题目' : '录入题目' }}</h3>
            <div class="admin-manage-row">
              <select v-model.number="selectedQuestionId" @change="loadSelectedQuestion">
                <option :value="null">新增题目</option>
                <option v-for="q in adminOptions.questions" :key="q.nodeId" :value="q.detailId">
                  {{ q.detailId || '未绑定ID' }} - {{ q.title }}
                </option>
              </select>
              <button class="admin-secondary-btn" @click="resetQuestionForm">新增</button>
              <button class="admin-danger-btn" :disabled="!selectedQuestionId" @click="deleteQuestion">删除</button>
            </div>
            <div class="form-grid">
              <label>题目 ID</label>
              <input v-model.number="questionForm.id" type="number" :disabled="!!selectedQuestionId" placeholder="对应 MySQL question_detail.id" />
              <label>题目标题</label>
              <input v-model="questionForm.title" placeholder="请输入题目标题" />
              <label>难度</label>
              <select v-model="questionForm.difficulty">
                <option value="Easy">简单</option>
                <option value="Normal">普通</option>
                <option value="Hard">困难</option>
              </select>
              <label>题目描述</label>
              <textarea v-model="questionForm.content" rows="5" placeholder="请输入题目描述"></textarea>
              <label>代码模板</label>
              <textarea v-model="questionForm.codeTemplate" rows="8" placeholder="请输入 Java 代码模板"></textarea>
              <label>期望输出</label>
              <textarea v-model="questionForm.expectedOutput" rows="3" placeholder="判题期望输出"></textarea>
              <label>时间限制(ms)</label>
              <input v-model.number="questionForm.timeLimit" type="number" />
              <label>内存限制(MB)</label>
              <input v-model.number="questionForm.memoryLimit" type="number" />
            </div>

            <div class="weight-editor">
              <div class="weight-title">
                <h4>所属知识点百分比</h4>
                <button @click="addKnowledgeWeight">新增知识点</button>
              </div>
              <div class="weight-row" v-for="(link, index) in questionForm.knowledgeLinks" :key="index">
                <select v-model.number="link.knowledgeId">
                  <option :value="null">请选择知识点</option>
                  <option v-for="kp in adminOptions.knowledges" :key="kp.nodeId" :value="kp.detailId">{{ kp.name }}</option>
                </select>
                <input v-model.number="link.weight" type="number" min="1" max="100" placeholder="百分比" />
                <span>%</span>
                <button class="remove-link-btn" @click="removeKnowledgeWeight(index)">删除</button>
              </div>
            </div>
            <button class="admin-primary-btn" @click="saveQuestion">{{ selectedQuestionId ? '保存修改' : '保存题目' }}</button>
          </div>

          <div v-show="adminView === 'module'" class="admin-form-card">
            <h3>{{ selectedModuleName ? '修改模块' : '录入模块' }}</h3>
            <div class="admin-manage-row">
              <select v-model="selectedModuleName" @change="loadSelectedModule">
                <option value="">新增模块</option>
                <option v-for="mod in adminOptions.modules" :key="mod.nodeId" :value="mod.name">{{ mod.name }}</option>
              </select>
              <button class="admin-secondary-btn" @click="resetModuleForm">新增</button>
              <button class="admin-danger-btn" :disabled="!selectedModuleName" @click="deleteModule">删除</button>
            </div>
            <div class="form-grid">
              <label>模块名称</label>
              <input v-model="moduleForm.name" placeholder="例如：面向对象三大特性" />
            </div>
            <button class="admin-primary-btn" @click="saveModule">{{ selectedModuleName ? '保存修改' : '保存模块' }}</button>
          </div>

          <div v-show="adminView === 'resource'" class="admin-form-card">
            <h3>{{ selectedResourceId ? '修改资源' : '录入资源' }}</h3>
            <div class="admin-manage-row">
              <select v-model.number="selectedResourceId" @change="loadSelectedResource">
                <option :value="null">新增资源</option>
                <option v-for="res in adminOptions.resources" :key="res.nodeId" :value="res.detailId">
                  {{ res.detailId || '未绑定ID' }} - {{ res.title }}
                </option>
              </select>
              <button class="admin-secondary-btn" @click="resetResourceForm">新增</button>
              <button class="admin-danger-btn" :disabled="!selectedResourceId" @click="deleteResource">删除</button>
            </div>
            <div class="form-grid">
              <label>资源 ID</label>
              <input v-model.number="resourceForm.id" type="number" :disabled="!!selectedResourceId" placeholder="资源唯一 ID" />
              <label>资源标题</label>
              <input v-model="resourceForm.title" placeholder="例如：视频：写循环" />
              <label>资源类型</label>
              <select v-model="resourceForm.resourceType">
                <option value="Video">视频</option>
                <option value="Article">图文</option>
              </select>
              <label>资源链接</label>
              <input v-model="resourceForm.url" placeholder="https://..." />
              <label>资源说明</label>
              <textarea v-model="resourceForm.description" rows="4" placeholder="填写视频或图文的补充说明"></textarea>
              <label>讲解知识点</label>
              <select v-model.number="resourceForm.knowledgeId">
                <option :value="null">请选择知识点</option>
                <option v-for="kp in adminOptions.knowledges" :key="kp.nodeId" :value="kp.detailId">
                  {{ kp.detailId || '未绑定ID' }} - {{ kp.name }}
                </option>
              </select>
            </div>
            <button class="admin-primary-btn" @click="saveResource">{{ selectedResourceId ? '保存修改' : '保存资源' }}</button>
          </div>

          <div v-show="adminView === 'user'" class="admin-form-card">
            <h3>用户管理</h3>
            <div class="user-admin-list">
              <div class="user-admin-row" v-for="user in adminOptions.users" :key="user.id" :class="{ current: user.current }">
                <div class="user-admin-meta">
                  <strong>{{ user.username }} <span>{{ roleName(user.role) }}</span></strong>
                  <small>ID: {{ user.id }}　手机号: {{ user.phone || '-' }}{{ user.current ? '　当前登录账号' : '' }}</small>
                </div>
                <button class="admin-secondary-btn" @click="selectUserForPassword(user)">重置密码</button>
                <button class="admin-danger-btn" :disabled="user.current" @click="deleteUser(user)">删除</button>
              </div>
            </div>

            <div v-if="selectedUserId" class="password-reset-box">
              <h4>重置 {{ selectedUserName }} 的密码</h4>
              <div class="form-grid">
                <label>新密码</label>
                <input v-model="userPasswordForm.newPassword" type="password" autocomplete="new-password" placeholder="至少 6 位" />
                <label>确认密码</label>
                <input v-model="userPasswordForm.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入新密码" />
              </div>
              <div class="password-actions">
                <button class="admin-primary-btn" @click="resetUserPassword">保存新密码</button>
                <button class="admin-secondary-btn" @click="cancelUserPasswordReset">取消</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template v-else>
    <div class="header">
      <div style="display: flex; align-items: center; gap: 20px;">
        <h2>📚 Java 编程练习平台</h2>
        <select v-model="currentModuleName" @change="handleModuleSelect($event)" class="header-select">
          <option value="ALL">全部图谱</option>
          <option v-for="mod in macroModules" :key="mod.id" :value="mod.label">
            {{ mod.label }}
          </option>
        </select>
      </div>
      <div style="display: flex; align-items: center; gap: 12px;">
        <span class="status-tag">Docker 沙箱已连接</span>
        <button class="logout-btn" @click="logout">退出登录</button>
      </div>
    </div>

    <div class="main-layout">
      <div class="sidebar">
        <div class="menu-item" :class="{ active: currentView === 'graph' }" @click="enterModule(currentModuleName)">
          🌌 知识图谱
        </div>
        <div class="menu-item" :class="{ active: currentView === 'knowledge' }" @click="switchView('knowledge')">
          📌 知识点
        </div>
        <div class="menu-item" :class="{ active: currentView === 'exercise' }" @click="switchView('exercise')">
          📝 练习题库
        </div>
        <div class="menu-item" :class="{ active: currentView === 'other' }" @click="switchView('other')">
          📂 其他
        </div>
      </div>

      <div class="content-area">

        <div v-show="currentView === 'graph'" class="graph-view" style="display: flex; height: 100%; width: 100%; gap: 20px;">
          <div class="left-graph" :style="{ width: (showEditor || showKnowledgePanel) ? '50%' : '100%' }" style="position: relative;">
            <div class="graph-legend">
              <div class="legend-title">📌 图例</div>
              <div class="legend-item"><span class="shape shape-macro"></span> 模块</div>
              <div class="legend-item"><span class="shape shape-micro"></span> 知识点</div>
              <div class="legend-item"><span class="shape shape-ex-easy"></span> 简单题</div>
              <div class="legend-item"><span class="shape shape-ex-normal"></span> 普通题</div>
              <div class="legend-item"><span class="shape shape-ex-hard"></span> 困难题</div>
              <div class="legend-item"><span class="shape shape-tut"></span> 资源</div>
            </div>
            <div :key="graphContainerKey" ref="graphContainerRef" class="graph-container"></div>
            <div v-show="isGraphLoading" class="graph-loading">图谱加载中...</div>
          </div>

          <div class="right-editor" v-if="showEditor">
            <div class="panel-header">
              <h3>📝 {{ currentQuestion.title || '题目详情' }}</h3>
              <span v-if="currentQuestion.difficulty === 'Easy'" class="tag-success">简单</span>
              <span v-else-if="currentQuestion.difficulty === 'Normal'" class="tag-warning">普通</span>
              <span v-else-if="currentQuestion.difficulty === 'Hard'" class="tag-danger">困难</span>
              <button class="close-btn" @click="closeGraphPanel">✖</button>
            </div>

            <div v-if="showDiagnosis" class="diagnosis-alert">
              <h4 style="margin-top:0"><span style="font-size:20px">🚨</span> 智能诊断报告</h4>
              <p>系统检测到您已连续错误 3 次。系统分析您可能在以下知识点存在盲区：</p>

              <div v-if="testedConcepts.length > 0" class="concept-options">
                <div v-for="(c, index) in testedConcepts" :key="c.id" class="concept-radio" @click="selectWeakPoint(c)">
                  <input type="radio" :checked="selectedWeakPoint && selectedWeakPoint.id === c.id" />
                  <span>{{ c.name }}</span>
                  <span class="weight-tag" :class="{'weight-high': c.weight >= 50}">权重: {{ c.weight }}%</span>
<!--                  <span v-if="index === 0" class="recommend-tag">🔥 最大嫌疑</span>-->
                </div>
                <div style="margin-top:10px; text-align: right;">
                   <button style="padding: 5px 10px; background: #409EFF; color: #fff; border: none; border-radius: 4px; cursor: pointer;" @click="selectWeakPoint(testedConcepts[0])">直接诊断问题</button>
                </div>
              </div>

              <div v-if="diagnosisResult.weakPoint" class="prescription-box">
                <h5 style="margin: 10px 0; color: #E6A23C;">📝 问题解决方法：</h5>
                <p style="margin: 5px 0; font-size: 14px;">病因锁定：<strong>【{{ diagnosisResult.weakPoint }}】</strong></p>

                <div v-if="diagnosisResult.recommendType === 'Exercise'">
                   <p style="margin: 5px 0; font-size: 14px;">建议退回学习：<strong>{{ diagnosisResult.recommendKnowledge }}</strong></p>
                   <p style="margin: 5px 0; font-size: 14px;">推荐先完成基础练习：
                     <a href="#" class="pulse-link" @click.prevent="openExerciseDetailOnly({id: diagnosisResult.recommendExerciseId, customId: diagnosisResult.recommendExerciseCustomId, label: diagnosisResult.recommendExerciseTitle})">
                       {{ diagnosisResult.recommendExerciseTitle }}
                     </a>
                   </p>
                </div>

                <div v-if="diagnosisResult.recommendType === 'Knowledge'" style="background: #e1f3d8; padding: 10px; border-radius: 4px; margin-top:10px;">
                   <p style="margin: 5px 0; color:#67C23A; font-weight:bold;">⚠️ 提醒：该知识点暂无更简单的降级题目！</p>
                   <p style="margin: 5px 0; font-size: 14px;">请直接前往学习知识点百科：</p>
                   <a href="#" class="tutorial-btn" style="background:#67C23A;" @click.prevent="openKnowledgeDetailOnly({id: diagnosisResult.recommendKnowledgeId, label: diagnosisResult.recommendKnowledgeName})">
                     📖 点击前往学习《{{ diagnosisResult.recommendKnowledgeName }}》
                   </a>
                </div>

                <div v-if="diagnosisResult.recommendType === 'Resource'" style="background: #fef0f0; padding: 10px; border-radius: 4px; margin-top:10px;">
                   <p style="margin: 5px 0; color:#F56C6C; font-weight:bold;">⚠️ 警告：已触及基础底线！做题毫无意义。</p>
                   <p style="margin: 5px 0; font-size: 14px;">请立即停止盲目猜测，前往学习基础资源：</p>
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
              <button class="close-btn" @click="closeGraphPanel">✖</button>
            </div>

            <div class="knowledge-desc">
              <p style="white-space: pre-wrap;">{{ currentKnowledge.description || '加载中，请稍候...' }}</p>
            </div>

            <div class="knowledge-video" v-if="currentKnowledge.videoUrl">
              <a :href="currentKnowledge.videoUrl" target="_blank" rel="noopener noreferrer">打开相关视频/资料</a>
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
                <span v-for="ex in currentKnowledge.relatedExercises" :key="ex.id" class="e-tag" @click="openEditor(ex.id, ex.title, ex.customId)">
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

        <div v-show="currentView === 'knowledge'" class="list-view">
          <h3>📌 知识点列表</h3>
          <div class="card-grid">
            <div class="info-card knowledge-card" :class="{ active: String(activeKnowledgeId) === String(kp.id) }" v-for="kp in knowledgePoints" :key="kp.id" @click="openKnowledgeFromList(kp)">
              <div class="card-title">{{ kp.label }}</div>
              <div class="card-type">类型: {{ kp.type === 'MicroConcept' ? '知识点' : kp.type }}</div>
              <button class="knowledge-btn">查看详情</button>
            </div>
          </div>
        </div>

        <div v-show="currentView === 'other'" class="list-view">
          <h3>📂 其他资源</h3>
          <div class="card-grid">
            <div class="info-card other-card" v-for="item in otherResources" :key="item.id" @click="openResourceFromList(item)">
              <div class="card-title">{{ item.label }}</div>
              <div class="card-type">类型: {{ item.type === 'Video' ? '视频' : '图文' }}</div>
              <button class="other-btn">{{ item.url ? '打开资源' : '查看节点' }}</button>
            </div>
          </div>
        </div>

        <div v-show="currentView === 'exerciseDetail'" class="detail-view">
          <div class="detail-header">
            <button class="back-btn" @click="goBackToList">返回上一级</button>
            <div class="detail-title">
              <h3>📝 {{ currentQuestion.title || '题目详情' }}</h3>
              <span v-if="currentQuestion.difficulty === 'Easy'" class="tag-success">简单</span>
              <span v-else-if="currentQuestion.difficulty === 'Normal'" class="tag-warning">普通</span>
              <span v-else-if="currentQuestion.difficulty === 'Hard'" class="tag-danger">困难</span>
            </div>
            <button class="locate-btn" @click="locateCurrentExercise">定位</button>
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
            </div>
          </div>

          <div class="question-desc">
            <p style="white-space: pre-wrap;"><strong>题目描述：</strong> {{ currentQuestion.content || '请根据面向对象知识，编写满足要求的 Java 代码。' }}</p>
          </div>
          <div class="code-area">
            <textarea v-model="code" rows="16" class="custom-editor"></textarea>
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

        <div v-show="currentView === 'knowledgeDetail'" class="detail-view">
          <div class="detail-header">
            <button class="back-btn" @click="goBackToList">返回上一级</button>
            <div class="detail-title">
              <h3>📖 {{ currentKnowledge.name || '知识点加载中...' }}</h3>
            </div>
            <button class="locate-btn" @click="locateCurrentKnowledge">定位</button>
          </div>

          <div class="knowledge-desc">
            <p style="white-space: pre-wrap;">{{ currentKnowledge.description || '加载中，请稍候...' }}</p>
          </div>

          <div class="knowledge-video" v-if="currentKnowledge.videoUrl">
            <a :href="currentKnowledge.videoUrl" target="_blank" rel="noopener noreferrer">打开相关视频/资料</a>
          </div>

          <div class="relation-section" v-if="currentKnowledge.preKnowledges && currentKnowledge.preKnowledges.length > 0">
            <h4 style="color: #67C23A; margin-bottom: 8px;">🔄 建议先掌握以下前置知识：</h4>
            <div class="tag-list">
              <span v-for="pre in currentKnowledge.preKnowledges" :key="pre.id" class="k-tag" @click="openKnowledgeDetailOnly(pre)">
                {{ pre.name }}
              </span>
            </div>
          </div>

          <div class="relation-section" v-if="currentKnowledge.relatedExercises && currentKnowledge.relatedExercises.length > 0" style="margin-top: 20px;">
            <h4 style="color: #F56C6C; margin-bottom: 8px;">🎯 巩固该知识点的相关练习题：</h4>
            <div class="tag-list">
              <span v-for="ex in currentKnowledge.relatedExercises" :key="ex.id" class="e-tag" @click="openExerciseDetailOnly({ id: ex.id, label: ex.title, customId: ex.customId })">
                {{ ex.title }}
              </span>
            </div>
          </div>
        </div>

      </div>
    </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { Graph } from '@antv/g6'
import axios from 'axios'
import { ElMessage } from 'element-plus'

localStorage.removeItem('kgOjUser')
const currentUser = ref(null)
const authMode = ref('login')
const authForm = ref({ username: '', phone: '', password: '', confirmPassword: '', role: 'STUDENT' })

const adminView = ref('knowledge')
const adminOptions = ref({ modules: [], knowledges: [], questions: [], resources: [], users: [] })
const selectedModuleName = ref('')
const selectedKnowledgeId = ref(null)
const selectedQuestionId = ref(null)
const selectedResourceId = ref(null)
const selectedUserId = ref(null)
const selectedUserName = ref('')
const moduleForm = ref({ originalName: '', name: '' })
const knowledgeForm = ref({ id: null, name: '', moduleName: '', preKnowledgeId: null, description: '', videoUrl: '' })
const resourceForm = ref({ id: null, title: '', resourceType: 'Video', url: '', knowledgeId: null, description: '' })
const userPasswordForm = ref({ newPassword: '', confirmPassword: '' })
const defaultCodeTemplate = 'public class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}'
const createDefaultQuestionForm = () => ({
  id: null,
  title: '',
  difficulty: 'Normal',
  content: '',
  codeTemplate: defaultCodeTemplate,
  expectedOutput: '',
  timeLimit: 1000,
  memoryLimit: 128,
  knowledgeLinks: [{ knowledgeId: null, weight: 100 }]
})
const questionForm = ref(createDefaultQuestionForm())

const currentView = ref('graph')
const macroModules = ref([])
const currentModuleName = ref('ALL')
const exercises = ref([])
const knowledgePoints = ref([])
const otherResources = ref([])
const graphContainerRef = ref(null)
const graphContainerKey = ref(0)
const isGraphLoading = ref(false)
const loadedGraphModule = ref(null)

const showEditor = ref(false)
const currentQuestion = ref({})
const code = ref('')
const isJudging = ref(false)
const judgeResult = ref('')
const isAC = ref(false)

const showKnowledgePanel = ref(false)
const currentKnowledge = ref({})
const previousListView = ref('exercise')
const currentExerciseNode = ref(null)
const currentKnowledgeNode = ref(null)
const activeKnowledgeId = ref(null)

const waCounts = ref({})
const showDiagnosis = ref(false)
const testedConcepts = ref([])
const selectedWeakPoint = ref(null)
const diagnosisResult = ref({})

let globalNodes = []
let graphInstance = null
let graphRenderVersion = 0
let graphLoadVersion = 0
let graphResizeObserver = null

const authHeaders = () => ({
  headers: {
    Authorization: 'Bearer ' + (currentUser.value?.token || '')
  }
})

const submitAuth = async () => {
  if (!authForm.value.username || !authForm.value.password) {
    ElMessage.warning('用户名和密码不能为空')
    return
  }
  if (authMode.value === 'register') {
    if (!authForm.value.phone) {
      ElMessage.warning('手机号不能为空')
      return
    }
    if (authForm.value.password !== authForm.value.confirmPassword) {
      ElMessage.warning('两次输入的密码不一致')
      return
    }
  }
  try {
    const url = authMode.value === 'login' ? '/api/auth/login' : '/api/auth/register'
    const res = await axios.post('http://localhost:8080' + url, authForm.value)
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '操作失败')
      return
    }
    currentUser.value = { id: res.data.id, username: res.data.username, phone: res.data.phone, role: res.data.role, token: res.data.token }
    ElMessage.success(authMode.value === 'login' ? '登录成功' : '注册成功')
    if (currentUser.value.role === 'ADMIN') {
      await loadAdminOptions()
    } else {
      await initPage()
    }
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const logout = () => {
  currentUser.value = null
  localStorage.removeItem('kgOjUser')
  authForm.value = { username: '', phone: '', password: '', confirmPassword: '', role: 'STUDENT' }
  selectedUserId.value = null
  selectedUserName.value = ''
  userPasswordForm.value = { newPassword: '', confirmPassword: '' }
  resetStudentState()
}

const loadAdminOptions = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/admin/options', authHeaders())
    if (res.data && res.data.success === false) {
      ElMessage.error(res.data.message || '请重新登录')
      logout()
      return
    }
    adminOptions.value = {
      modules: res.data?.modules || [],
      knowledges: res.data?.knowledges || [],
      questions: res.data?.questions || [],
      resources: res.data?.resources || [],
      users: res.data?.users || []
    }
  } catch (e) {
    ElMessage.error('加载后台选项失败: ' + e.message)
  }
}

const resetModuleForm = () => {
  selectedModuleName.value = ''
  moduleForm.value = { originalName: '', name: '' }
}

const loadSelectedModule = () => {
  if (!selectedModuleName.value) {
    resetModuleForm()
    return
  }
  moduleForm.value = { originalName: selectedModuleName.value, name: selectedModuleName.value }
}

const saveModule = async () => {
  if (!moduleForm.value.name) {
    ElMessage.warning('模块名称不能为空')
    return
  }
  try {
    const res = await axios.post('http://localhost:8080/api/admin/module', moduleForm.value, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '保存失败')
      return
    }
    ElMessage.success('模块保存成功')
    resetModuleForm()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const deleteModule = async () => {
  if (!selectedModuleName.value) return
  if (!confirm('确定删除该模块吗？知识点不会被删除，但所属关系会被移除。')) return
  try {
    const res = await axios.delete('http://localhost:8080/api/admin/module/' + encodeURIComponent(selectedModuleName.value), authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '删除失败')
      return
    }
    ElMessage.success('模块删除成功')
    resetModuleForm()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const resetKnowledgeForm = () => {
  selectedKnowledgeId.value = null
  knowledgeForm.value = { id: null, name: '', moduleName: '', preKnowledgeId: null, description: '', videoUrl: '' }
}

const loadSelectedKnowledge = () => {
  if (!selectedKnowledgeId.value) {
    resetKnowledgeForm()
    return
  }
  const kp = adminOptions.value.knowledges.find(item => Number(item.detailId) === Number(selectedKnowledgeId.value))
  if (!kp) return
  knowledgeForm.value = {
    id: Number(kp.detailId),
    name: kp.name || '',
    moduleName: kp.moduleName || '',
    preKnowledgeId: kp.preKnowledgeId == null ? null : Number(kp.preKnowledgeId),
    description: kp.description || '',
    videoUrl: kp.videoUrl || ''
  }
}

const saveKnowledge = async () => {
  if (!knowledgeForm.value.id || !knowledgeForm.value.name) {
    ElMessage.warning('知识点 ID 和名称不能为空')
    return
  }
  try {
    const res = await axios.post('http://localhost:8080/api/admin/knowledge', knowledgeForm.value, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '保存失败')
      return
    }
    ElMessage.success('知识点保存成功')
    resetKnowledgeForm()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const deleteKnowledge = async () => {
  if (!selectedKnowledgeId.value) return
  if (!confirm('确定删除该知识点吗？相关前置关系和题目测试关系也会被移除。')) return
  try {
    const res = await axios.delete('http://localhost:8080/api/admin/knowledge/' + selectedKnowledgeId.value, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '删除失败')
      return
    }
    ElMessage.success('知识点删除成功')
    resetKnowledgeForm()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const resetQuestionForm = () => {
  selectedQuestionId.value = null
  questionForm.value = createDefaultQuestionForm()
}

const loadSelectedQuestion = () => {
  if (!selectedQuestionId.value) {
    resetQuestionForm()
    return
  }
  const question = adminOptions.value.questions.find(item => Number(item.detailId) === Number(selectedQuestionId.value))
  if (!question) return
  const links = (question.knowledgeLinks || []).map(link => ({
    knowledgeId: link.knowledgeId == null ? null : Number(link.knowledgeId),
    weight: link.weight == null ? 100 : Number(link.weight)
  }))
  questionForm.value = {
    id: Number(question.detailId),
    title: question.title || '',
    difficulty: question.difficulty || 'Normal',
    content: question.content || '',
    codeTemplate: question.codeTemplate || defaultCodeTemplate,
    expectedOutput: question.expectedOutput || '',
    timeLimit: question.timeLimit || 1000,
    memoryLimit: question.memoryLimit || 128,
    knowledgeLinks: links.length > 0 ? links : [{ knowledgeId: null, weight: 100 }]
  }
}

const addKnowledgeWeight = () => {
  questionForm.value.knowledgeLinks.push({ knowledgeId: null, weight: 100 })
}

const removeKnowledgeWeight = (index) => {
  if (questionForm.value.knowledgeLinks.length === 1) {
    ElMessage.warning('至少保留一个知识点')
    return
  }
  questionForm.value.knowledgeLinks.splice(index, 1)
}

const saveQuestion = async () => {
  if (!questionForm.value.id || !questionForm.value.title) {
    ElMessage.warning('题目 ID 和标题不能为空')
    return
  }
  const invalidLink = questionForm.value.knowledgeLinks.some(link => !link.knowledgeId || !link.weight)
  if (invalidLink) {
    ElMessage.warning('请填写完整的知识点和百分比')
    return
  }
  const totalWeight = questionForm.value.knowledgeLinks.reduce((sum, link) => sum + Number(link.weight || 0), 0)
  if (totalWeight > 100) {
    ElMessage.warning('知识点百分比总和不能超过 100%')
    return
  }
  try {
    const res = await axios.post('http://localhost:8080/api/admin/question', questionForm.value, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '保存失败')
      return
    }
    ElMessage.success('题目保存成功')
    resetQuestionForm()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const deleteQuestion = async () => {
  if (!selectedQuestionId.value) return
  if (!confirm('确定删除该题目吗？题目详情和图谱中的题目节点都会被删除。')) return
  try {
    const res = await axios.delete('http://localhost:8080/api/admin/question/' + selectedQuestionId.value, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '删除失败')
      return
    }
    ElMessage.success('题目删除成功')
    resetQuestionForm()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const resetResourceForm = () => {
  selectedResourceId.value = null
  resourceForm.value = { id: null, title: '', resourceType: 'Video', url: '', knowledgeId: null, description: '' }
}

const loadSelectedResource = () => {
  if (!selectedResourceId.value) {
    resetResourceForm()
    return
  }
  const resource = adminOptions.value.resources.find(item => Number(item.detailId) === Number(selectedResourceId.value))
  if (!resource) return
  resourceForm.value = {
    id: Number(resource.detailId),
    title: resource.title || '',
    resourceType: resource.resourceType === 'Article' ? 'Article' : 'Video',
    url: resource.url || '',
    knowledgeId: resource.knowledgeId == null ? null : Number(resource.knowledgeId),
    description: resource.description || ''
  }
}

const saveResource = async () => {
  if (!resourceForm.value.id || !resourceForm.value.title) {
    ElMessage.warning('资源 ID 和标题不能为空')
    return
  }
  if (!resourceForm.value.knowledgeId) {
    ElMessage.warning('请选择该资源讲解的知识点')
    return
  }
  try {
    const res = await axios.post('http://localhost:8080/api/admin/resource', resourceForm.value, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '保存失败')
      return
    }
    ElMessage.success('资源保存成功')
    resetResourceForm()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const deleteResource = async () => {
  if (!selectedResourceId.value) return
  if (!confirm('确定删除该资源吗？')) return
  try {
    const res = await axios.delete('http://localhost:8080/api/admin/resource/' + selectedResourceId.value, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '删除失败')
      return
    }
    ElMessage.success('资源删除成功')
    resetResourceForm()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const roleName = (role) => role === 'ADMIN' ? '管理员' : '学生'

const selectUserForPassword = (user) => {
  selectedUserId.value = user.id
  selectedUserName.value = user.username
  userPasswordForm.value = { newPassword: '', confirmPassword: '' }
}

const cancelUserPasswordReset = () => {
  selectedUserId.value = null
  selectedUserName.value = ''
  userPasswordForm.value = { newPassword: '', confirmPassword: '' }
}

const resetUserPassword = async () => {
  if (!selectedUserId.value) return
  if (!userPasswordForm.value.newPassword || userPasswordForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少需要 6 位')
    return
  }
  if (userPasswordForm.value.newPassword !== userPasswordForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  try {
    const res = await axios.post('http://localhost:8080/api/admin/user/' + selectedUserId.value + '/password', userPasswordForm.value, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '重置失败')
      return
    }
    ElMessage.success('密码重置成功')
    cancelUserPasswordReset()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const deleteUser = async (user) => {
  if (!user || user.current) return
  if (!confirm('确定删除账号「' + user.username + '」吗？删除后该账号不能再登录。')) return
  try {
    const res = await axios.delete('http://localhost:8080/api/admin/user/' + user.id, authHeaders())
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '删除失败')
      return
    }
    ElMessage.success('账号删除成功')
    if (selectedUserId.value === user.id) cancelUserPasswordReset()
    await loadAdminOptions()
  } catch (e) {
    ElMessage.error('服务器请求失败: ' + e.message)
  }
}

const destroyGraph = () => {
  if (graphInstance) {
    graphInstance.destroy()
    graphInstance = null
  }
  loadedGraphModule.value = null
  globalNodes = []
}

const resetStudentState = () => {
  if (graphResizeObserver) {
    graphResizeObserver.disconnect()
    graphResizeObserver = null
  }
  destroyGraph()
  graphRenderVersion++
  graphLoadVersion++
  graphContainerKey.value++
  currentView.value = 'graph'
  currentModuleName.value = 'ALL'
  exercises.value = []
  knowledgePoints.value = []
  otherResources.value = []
  showEditor.value = false
  showKnowledgePanel.value = false
  currentQuestion.value = {}
  currentKnowledge.value = {}
  currentExerciseNode.value = null
  currentKnowledgeNode.value = null
  activeKnowledgeId.value = null
  judgeResult.value = ''
  isAC.value = false
  showDiagnosis.value = false
}

const resizeGraphToContainer = async () => {
  if (!graphInstance) return
  await nextTick()
  const resizeOnce = () => {
    if (!graphInstance || graphInstance.destroyed) return
    const container = graphContainerRef.value
    if (!container) return
    graphInstance.resize(container.clientWidth || container.scrollWidth, container.clientHeight || container.scrollHeight)
    graphInstance.fitView().catch(() => {})
  }
  requestAnimationFrame(resizeOnce)
  setTimeout(resizeOnce, 80)
  setTimeout(resizeOnce, 320)
}

const closeGraphPanel = async () => {
  showEditor.value = false
  showKnowledgePanel.value = false
  await resizeGraphToContainer()
}

const bindGraphResizeObserver = async () => {
  await nextTick()
  if (graphResizeObserver) {
    graphResizeObserver.disconnect()
    graphResizeObserver = null
  }
  const container = graphContainerRef.value
  if (!container || typeof ResizeObserver === 'undefined') return
  graphResizeObserver = new ResizeObserver(() => {
    if (currentView.value === 'graph') resizeGraphToContainer()
  })
  graphResizeObserver.observe(container)
}

const switchView = async (viewName) => {
  currentView.value = viewName
  if (viewName === 'graph' && graphInstance) {
    await resizeGraphToContainer()
  } else if (viewName !== 'graph') {
    showEditor.value = false
    showKnowledgePanel.value = false
    if (viewName === 'exercise' || viewName === 'knowledge' || viewName === 'other') {
      await loadGraphData()
    }
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

const handleModuleSelect = async (event) => {
  const selectedModule = event?.target?.value || currentModuleName.value
  currentModuleName.value = selectedModule

  if (currentView.value === 'graph') {
    await loadGraph(selectedModule)
  } else {
    await loadGraphData(selectedModule)
  }
}

const enterModule = async (moduleName) => {
  currentModuleName.value = moduleName
  await loadGraph(moduleName)
}

const fetchGraphData = async (moduleName = currentModuleName.value) => {
  const params = new URLSearchParams({
    module: moduleName
  })
  const response = await axios.get('http://localhost:8080/api/graph/all?' + params.toString())
  return response.data
}

const loadGraphData = async (moduleName = currentModuleName.value) => {
  try {
    const data = await fetchGraphData(moduleName)
    updateNodeLists(data)
    return data
  } catch (e) {
    alert("加载图谱失败")
    return null
  }
}

const loadGraph = async (moduleName = currentModuleName.value) => {
  const loadVersion = ++graphLoadVersion
  currentView.value = 'graph'
  showEditor.value = false
  showKnowledgePanel.value = false
  const moduleToRender = moduleName
  currentModuleName.value = moduleToRender
  try {
    if (graphInstance && loadedGraphModule.value === moduleToRender) {
      await nextTick()
      const container = graphContainerRef.value
      if (container) {
        graphInstance.resize(container.clientWidth || container.scrollWidth, container.clientHeight || container.scrollHeight)
        await graphInstance.fitCenter()
      }
      isGraphLoading.value = false
      return
    }

    isGraphLoading.value = true
    destroyGraph()
    graphContainerKey.value++
    loadedGraphModule.value = null
    await nextTick()
    const data = await fetchGraphData(moduleToRender)
    if (loadVersion !== graphLoadVersion || moduleToRender !== currentModuleName.value) return
    updateNodeLists(data)
    isGraphLoading.value = false
    graphContainerKey.value++
    await nextTick()
    await bindGraphResizeObserver()
    await renderGraph(data, moduleToRender === 'ALL', loadVersion)
    loadedGraphModule.value = moduleToRender
  } catch (e) {
    alert("加载图谱失败")
  } finally {
    if (loadVersion === graphLoadVersion) isGraphLoading.value = false
  }
}

const initPage = async () => {
  await fetchLobbyData()
  await loadGraph()
}

const initByRole = async () => {
  if (!currentUser.value) return
  if (currentUser.value.role === 'ADMIN') {
    await loadAdminOptions()
  } else {
    await initPage()
  }
}

const updateNodeLists = (rawData) => {
  const nodes = rawData?.nodes || []
  const seenExercises = new Set()
  const seenKnowledge = new Set()
  const seenResources = new Set()

  exercises.value = nodes
    .filter(n => n.id && n.label && n.type === 'Exercise')
    .filter(n => {
      if (seenExercises.has(n.id)) return false
      seenExercises.add(n.id)
      return true
    })
    .map(n => ({
      id: n.id,
      label: n.label,
      type: n.type,
      difficulty: n.difficulty,
      customId: n.exId
    }))

  knowledgePoints.value = nodes
    .filter(n => n.id && n.label && (n.type === 'MicroConcept' || n.type === 'KnowledgePoint'))
    .filter(n => {
      if (seenKnowledge.has(n.id)) return false
      seenKnowledge.add(n.id)
      return true
    })
    .map(n => ({
      id: n.id,
      label: n.label,
      type: n.type,
      detailId: n.detailId
    }))

  otherResources.value = nodes
    .filter(n => n.id && n.label && (n.type === 'Video' || n.type === 'Article'))
    .filter(n => {
      if (seenResources.has(n.id)) return false
      seenResources.add(n.id)
      return true
    })
    .map(n => ({
      id: n.id,
      label: n.label,
      type: n.type,
      url: n.url
    }))
}

const renderGraph = async (rawData, isAll = false, loadVersion = graphLoadVersion) => {
  const renderVersion = ++graphRenderVersion
  await nextTick()
  const container = graphContainerRef.value
  if (!container) return

  const width = container.clientWidth || container.scrollWidth || 800
  const height = container.clientHeight || container.scrollHeight || 600
  if (width === 0 || height === 0) return

  const formattedNodes = []
  const nodeIds = new Set()

  rawData.nodes.forEach(n => {
    if (n.id && n.label && !nodeIds.has(n.id)) {
      nodeIds.add(n.id)

      let color = '#909399'; let shapeType = 'circle'; let radius = 25;

      const nodeLabel = String(n.label)

      if (n.type === 'MacroConcept') { shapeType = 'circle'; color = '#E6A23C'; radius = 45; }
      else if (n.type === 'MicroConcept' || n.type === 'KnowledgePoint') { shapeType = 'circle'; color = '#409EFF'; radius = 30; }
      else if (n.type === 'Exercise') {
        shapeType = 'triangle';
        if(n.difficulty === 'Easy') color = '#67C23A'; else if (n.difficulty === 'Hard') color = '#F56C6C'; else color = '#E6A23C';
      } else if (n.type === 'Video' || n.type === 'Article') { shapeType = 'rect'; color = '#909399'; }

      const nodeObj = {
        id: String(n.id), type: shapeType,
        data: { label: nodeLabel.substring(0, 15), type: n.type, difficulty: n.difficulty, customId: n.exId, detailId: n.detailId },
        style: { fill: color, cursor: 'pointer' }
      }

      if (shapeType === 'circle') { nodeObj.style.r = radius }
      else if (shapeType === 'triangle') { nodeObj.style.direction = 'up'; nodeObj.style.size = 35 }
      else { nodeObj.style.width = 100; nodeObj.style.height = 30; nodeObj.style.radius = 4; }

      formattedNodes.push(nodeObj)
    }
  })

  globalNodes = formattedNodes;

  const formattedEdges = []
  rawData.edges.forEach(e => {
    if (nodeIds.has(String(e.source)) && nodeIds.has(String(e.target))) {
      let edgeColor = '#Dcdcdc';
      const edgeLabel = e.label || '';
      if (edgeLabel.includes('TESTS')) edgeColor = '#F56C6C';
      if (edgeLabel.includes('PRE')) edgeColor = '#409EFF';
      if (edgeLabel.includes('BELONGS')) edgeColor = '#E6A23C';
      if (edgeLabel.includes('EXPL')) edgeColor = '#909399';

      formattedEdges.push({
        source: String(e.source), target: String(e.target),
        data: { label: edgeLabel },
        style: { stroke: edgeColor, lineWidth: e.weight ? (e.weight/20) : 2 }
      })
    }
  })

  const dynamicNodeStrength = isAll ? 2000 : 800;
  const dynamicLinkDistance = isAll ? 250 : 150;

  if (renderVersion !== graphRenderVersion || loadVersion !== graphLoadVersion) return

  graphInstance = new Graph({
    container, width, height,
    fitView: true, fitViewPadding: [40, 40, 40, 40],
    data: { nodes: formattedNodes, edges: formattedEdges },
    node: { style: { labelText: (d) => d.data.label, labelPlacement: 'bottom', labelFontSize: 13, labelFill: '#333' } },
    edge: { style: { labelText: (d) => d.data.label, labelBackground: true, labelBackgroundFill: '#fff', labelFontSize: 10, endArrow: true } },
    layout: { type: 'force', linkDistance: dynamicLinkDistance, preventOverlap: true, nodeStrength: dynamicNodeStrength, collideStrength: 1, alphaDecay: 0.02 },
    behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element']
  })

  await graphInstance.render()
  if (renderVersion !== graphRenderVersion || loadVersion !== graphLoadVersion || !graphInstance || graphInstance.destroyed) return

  requestAnimationFrame(() => {
    if (loadVersion !== graphLoadVersion || !graphInstance || graphInstance.destroyed) return
    const latestContainer = graphContainerRef.value
    if (!latestContainer) return
    graphInstance.resize(latestContainer.clientWidth || width, latestContainer.clientHeight || height)
    graphInstance.fitView().catch(() => {})
  })

  graphInstance.on('node:click', (e) => {
    const nodeId = e.target.id;
    const clickedNode = globalNodes.find(n => n.id === nodeId);
    if (clickedNode) {
      if (clickedNode.data.type === 'Exercise') {
        openEditor(clickedNode.id, clickedNode.data.label, clickedNode.data.customId);
      } else if (clickedNode.data.type === 'MicroConcept' || clickedNode.data.type === 'MacroConcept' || clickedNode.data.type === 'KnowledgePoint') {
        openKnowledgePanel(clickedNode.id, undefined, clickedNode.data.detailId);
      } else if (clickedNode.data.type === 'Video' || clickedNode.data.type === 'Article') {
        const tut = rawData.nodes.find(n => String(n.id) === nodeId);
        if(tut && tut.url) window.open(tut.url, '_blank');
        else ElMessage.info("该资源暂时没有链接");
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
  if (concept?.id) activeKnowledgeId.value = concept.id
  try {
    const res = await axios.get("http://localhost:8080/api/diagnosis/recommend?conceptId=" + concept.id + "&conceptName=" + encodeURIComponent(concept.name) + "&currentExerciseId=" + (currentQuestion.value.id || ""))
    diagnosisResult.value = res.data

    if (res.data.recommendType === 'Exercise' && res.data.recommendExerciseId) {
      highlightGraphNode(String(res.data.recommendExerciseId), '#FFD700')
    } else if (res.data.recommendType === 'Knowledge' && res.data.recommendKnowledgeId) {
      activeKnowledgeId.value = res.data.recommendKnowledgeId
      highlightGraphNode(String(res.data.recommendKnowledgeId), '#FFD700')
    } else if (res.data.recommendType === 'Resource' && res.data.tutorialId) {
      highlightGraphNode(String(res.data.tutorialId), '#F56C6C')
    }
  } catch (e) {
    console.error('开药方失败', e)
  }
}

const markTutorialClicked = (tutId) => {
   ElMessage.success("态度不错！去好好看资源吧，看完再来战！");
   highlightGraphNode(String(tutId), '#67C23A');
}

const loadExerciseDetail = async (nodeId, nodeLabel, customId) => {
  currentExerciseNode.value = { id: nodeId, label: nodeLabel, customId }
  currentQuestion.value = { id: nodeId, title: nodeLabel, customId: customId }
  code.value = "正在加载题目详情..."
  showDiagnosis.value = false;

  const fetchId = customId || nodeId;
  try {
    const res = await axios.get('http://localhost:8080/api/question/' + fetchId)
    if (res.data && res.data.title) { currentQuestion.value = res.data; currentQuestion.value.id = nodeId; currentQuestion.value.customId = customId; code.value = res.data.codeTemplate || 'public class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}' }
    else { code.value = 'public class Main {\n    public static void main(String[] args) {\n        // 数据库中暂无该题目详情\n    }\n}' }
  } catch (e) { code.value = 'public class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}' }
}

const openEditor = async (nodeId, nodeLabel, customId) => {
  showKnowledgePanel.value = false
  showEditor.value = true
  await loadExerciseDetail(nodeId, nodeLabel, customId)
  await resizeGraphToContainer()
  highlightGraphNode(String(nodeId), '#F56C6C')
}

const loadKnowledgeDetail = async (nodeId, nodeLabel, detailId) => {
  activeKnowledgeId.value = nodeId
  currentKnowledgeNode.value = { id: nodeId, label: nodeLabel, detailId }
  currentKnowledge.value = { name: nodeLabel || '加载中...' }
  const fetchId = detailId || nodeId || nodeLabel
  try {
    const res = await axios.get('http://localhost:8080/api/knowledge/detail/' + encodeURIComponent(fetchId));
    if (res.data && res.data.name) currentKnowledge.value = res.data;
    else currentKnowledge.value = { name: '获取信息失败', description: '未能找到该节点的关联信息。' };
  } catch (e) { currentKnowledge.value = { name: '服务器异常', description: '请检查后端服务。' }; }
}

const openKnowledgePanel = async (nodeId, nodeLabel, detailId) => {
  showEditor.value = false
  showKnowledgePanel.value = true
  activeKnowledgeId.value = nodeId
  await loadKnowledgeDetail(nodeId, nodeLabel, detailId)
  await resizeGraphToContainer()
  highlightGraphNode(String(nodeId), '#FFD700')
}

const openEditorFromList = async (exNode) => {
  previousListView.value = 'exercise'
  currentView.value = 'exerciseDetail'
  showEditor.value = false
  showKnowledgePanel.value = false
  await loadExerciseDetail(exNode.id, exNode.label || exNode.title, exNode.customId)
}

const openKnowledgeFromList = async (kpNode) => {
  previousListView.value = 'knowledge'
  currentView.value = 'knowledgeDetail'
  showEditor.value = false
  showKnowledgePanel.value = false
  await loadKnowledgeDetail(kpNode.id, kpNode.label || kpNode.name, kpNode.detailId)
}

const openExerciseDetailOnly = async (exNode) => {
  previousListView.value = currentView.value === 'knowledgeDetail' ? 'knowledgeDetail' : 'exercise'
  currentView.value = 'exerciseDetail'
  await loadExerciseDetail(exNode.id, exNode.label || exNode.title, exNode.customId)
}

const openKnowledgeDetailOnly = async (kpNode) => {
  previousListView.value = currentView.value === 'knowledgeDetail' ? 'knowledgeDetail' : 'knowledge'
  currentView.value = 'knowledgeDetail'
  await loadKnowledgeDetail(kpNode.id, kpNode.label || kpNode.name, kpNode.detailId)
}

const goBackToList = async () => {
  if (previousListView.value === 'knowledgeDetail') {
    currentView.value = 'knowledge'
  } else {
    currentView.value = previousListView.value || 'exercise'
  }
  showEditor.value = false
  showKnowledgePanel.value = false
  await loadGraphData(currentModuleName.value)
}

const locateCurrentExercise = async () => {
  const ex = currentExerciseNode.value
  if (!ex) return
  await loadGraph(currentModuleName.value)
  await openEditor(ex.id, ex.label || currentQuestion.value.title, ex.customId)
}

const locateCurrentKnowledge = async () => {
  const kp = currentKnowledgeNode.value
  if (!kp) return
  activeKnowledgeId.value = kp.id
  await loadGraph(currentModuleName.value)
  await openKnowledgePanel(kp.id, kp.label || currentKnowledge.value.name, kp.detailId)
}

const openResourceFromList = async (resourceNode) => {
  if (resourceNode.url) {
    window.open(resourceNode.url, '_blank')
    highlightGraphNode(String(resourceNode.id), '#909399')
    return
  }

  currentView.value = 'graph'
  await nextTick()
  if (!graphInstance) await loadGraph()
  highlightGraphNode(String(resourceNode.id), '#909399')
}

const highlightGraphNode = (nodeId, highlightColor) => {
  if (!graphInstance) return
  try {
    const nodesData = graphInstance.getNodeData()
    const updatedNodes = nodesData.map(n => {
      if (String(n.id) === String(nodeId)) return { id: n.id, style: { ...n.style, stroke: highlightColor, lineWidth: 8, shadowColor: highlightColor, shadowBlur: 28 } }
      else return { id: n.id, style: { ...n.style, stroke: 'none', lineWidth: 0, shadowColor: 'transparent', shadowBlur: 0 } }
    })
    graphInstance.updateNodeData(updatedNodes); graphInstance.draw()
  } catch (e) {}
}

onMounted(() => { initByRole() })
onUnmounted(() => {
  if (graphResizeObserver) {
    graphResizeObserver.disconnect()
    graphResizeObserver = null
  }
  destroyGraph()
})
</script>

<style>
body { margin: 0; background-color: #f5f7fa; font-family: sans-serif; }
.app-container { height: 100vh; display: flex; flex-direction: column; overflow: hidden; }
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #7da8a2; padding: clamp(14px, 3vw, 34px); box-sizing: border-box; overflow: auto; }
.auth-stage { width: min(100%, 980px); aspect-ratio: 1.72 / 1; min-height: 460px; max-height: calc(100vh - 56px); display: grid; grid-template-columns: minmax(330px, 45%) minmax(390px, 55%); border-radius: 4px; overflow: hidden; box-shadow: 0 26px 60px rgba(31,45,61,0.18); background: #f7b82f; }
.auth-visual { position: relative; background: #0f565c; color: #fff; overflow: hidden; }
.auth-visual::after { content: ""; position: absolute; top: 0; right: -72px; width: 145px; height: 100%; background: #f7b82f; transform: skewX(-5deg); transform-origin: top; z-index: 3; }
.visual-title { position: absolute; left: clamp(24px, 4vw, 44px); top: clamp(24px, 4vw, 42px); z-index: 2; max-width: 300px; }
.visual-title h2 { margin: 0 0 8px; font-size: clamp(20px, 2.4vw, 24px); letter-spacing: 0; }
.visual-title p { margin: 0; color: rgba(255,255,255,0.7); font-size: 14px; line-height: 1.6; }
.door-scene { position: absolute; left: clamp(26px, 6vw, 58px); bottom: clamp(52px, 9vw, 94px); width: 320px; height: 300px; z-index: 2; transform: scale(clamp(0.78, 0.09vw + 0.75, 1)); transform-origin: left bottom; }
.door-light { position: absolute; left: 42px; top: 48px; width: 110px; height: 242px; background: #ffe76e; box-shadow: 0 0 38px rgba(255,231,110,0.28); }
.door { position: absolute; left: 120px; top: 34px; width: 145px; height: 260px; background: #17777d; box-shadow: 10px 12px 0 rgba(0,0,0,0.12); }
.door-panel { position: absolute; width: 36px; height: 82px; background: #24959b; }
.door-panel.top-left { left: 24px; top: 35px; }
.door-panel.top-right { right: 24px; top: 35px; }
.door-panel.bottom-left { left: 24px; bottom: 32px; }
.door-panel.bottom-right { right: 24px; bottom: 32px; }
.door-knob { position: absolute; left: 16px; top: 142px; width: 11px; height: 11px; border-radius: 50%; background: #36aeb2; box-shadow: 0 0 0 3px rgba(255,255,255,0.08); }
.door-step { position: absolute; left: 12px; bottom: 0; width: 260px; height: 14px; background: #173f44; box-shadow: 0 5px 0 rgba(0,0,0,0.18); }
.auth-main { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: clamp(26px, 5vw, 42px) clamp(28px, 5vw, 50px) clamp(26px, 4vw, 34px) clamp(54px, 8vw, 86px); box-sizing: border-box; min-width: 0; }
.auth-panel { width: min(380px, 100%); background: #fff; border-radius: 6px; box-shadow: 0 18px 38px rgba(90,72,26,0.18); padding: 30px 30px 28px; box-sizing: border-box; }
.auth-card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.auth-card-head span { color: #d99616; font-size: 12px; font-weight: bold; letter-spacing: 1.6px; }
.auth-card-head small { color: #b8bcc4; font-size: 11px; }
.role-switch { display: flex; justify-content: center; gap: 30px; margin: 6px 0 4px; }
.role-switch label { display: inline-flex; align-items: center; gap: 7px; color: #606266; font-size: 13px; font-weight: bold; cursor: pointer; }
.role-switch input { display: none; }
.role-switch span { width: 13px; height: 13px; border-radius: 50%; border: 2px solid #c0c4cc; box-sizing: border-box; position: relative; }
.role-switch label.active { color: #195a63; }
.role-switch label.active span { border-color: #195a63; }
.role-switch label.active span::after { content: ""; position: absolute; width: 5px; height: 5px; border-radius: 50%; background: #195a63; top: 2px; left: 2px; }
.auth-form { display: flex; flex-direction: column; gap: 9px; }
.auth-form input { padding: 12px 13px; border: 1px solid #eef0f4; border-radius: 4px; outline: none; font-size: 14px; background: #fbfcfe; color: #303133; }
.auth-form input:focus { border-color: #195a63; box-shadow: 0 0 0 2px rgba(25,90,99,0.08); }
.auth-submit { margin-top: 14px; padding: 13px; border: none; border-radius: 3px; background: #195a63; color: #fff; font-weight: bold; cursor: pointer; font-size: 14px; }
.auth-submit:hover { background: #104851; }
.auth-link-btn { display: block; margin: 22px auto 0; border: none; background: transparent; color: #111; cursor: pointer; font-weight: bold; line-height: 1.5; }
.auth-link-btn:hover { color: #195a63; }
@media (max-width: 760px) {
  .auth-page { padding: 16px; }
  .auth-stage { width: min(100%, 430px); min-height: 650px; max-height: none; height: auto; aspect-ratio: auto; grid-template-columns: 1fr; }
  .auth-visual { min-height: 230px; }
  .auth-visual::after { display: none; }
  .visual-title { left: 24px; top: 24px; }
  .door-scene { transform: scale(0.62); transform-origin: left bottom; left: 18px; bottom: 22px; }
  .auth-main { padding: 34px 22px; }
}
@media (min-width: 761px) and (max-height: 620px) {
  .auth-stage { min-height: 440px; }
  .door-scene { bottom: 36px; transform: scale(0.74); }
  .visual-title { top: 24px; }
}
.logout-btn { padding: 8px 12px; border: none; border-radius: 4px; background: #909399; color: #fff; cursor: pointer; font-weight: bold; }
.admin-shell { min-height: 100vh; background: #f5f7fa; display: flex; flex-direction: column; }
.admin-header { height: 64px; background: #fff; box-shadow: 0 2px 12px rgba(0,0,0,0.08); display: flex; align-items: center; justify-content: space-between; padding: 0 24px; flex-shrink: 0; }
.admin-header h2 { margin: 0 0 4px; color: #303133; }
.admin-header span { color: #909399; font-size: 13px; }
.admin-layout { flex: 1; min-height: 0; display: flex; }
.admin-sidebar { width: 180px; background: #2c3e50; padding: 20px 12px; box-sizing: border-box; display: flex; flex-direction: column; gap: 10px; }
.admin-sidebar button { text-align: left; padding: 12px 14px; border: none; border-radius: 4px; background: transparent; color: #bfcbd9; cursor: pointer; font-weight: bold; }
.admin-sidebar button.active, .admin-sidebar button:hover { background: #1f2d3d; color: #409EFF; }
.admin-content { flex: 1; min-width: 0; overflow-y: auto; padding: 24px; box-sizing: border-box; }
.admin-form-card { background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); padding: 22px; max-width: 920px; }
.admin-form-card h3 { margin: 0 0 20px; color: #303133; padding-bottom: 10px; border-bottom: 2px solid #ebeef5; }
.admin-manage-row { display: grid; grid-template-columns: minmax(0, 1fr) 88px 88px; gap: 10px; margin-bottom: 18px; }
.admin-manage-row select { min-width: 0; border: 1px solid #dcdfe6; border-radius: 4px; padding: 9px 10px; font-size: 14px; outline: none; }
.admin-secondary-btn, .admin-danger-btn { border: none; border-radius: 4px; color: #fff; cursor: pointer; font-weight: bold; }
.admin-secondary-btn { background: #67C23A; }
.admin-danger-btn { background: #F56C6C; }
.admin-secondary-btn:hover { background: #529b2e; }
.admin-danger-btn:hover { background: #c45656; }
.admin-danger-btn:disabled { background: #fab6b6; cursor: not-allowed; }
.form-grid { display: grid; grid-template-columns: 140px minmax(0, 1fr); gap: 12px 14px; align-items: center; }
.form-grid label { color: #606266; font-weight: bold; }
.form-grid input, .form-grid select, .form-grid textarea { width: 100%; box-sizing: border-box; border: 1px solid #dcdfe6; border-radius: 4px; padding: 9px 10px; font-size: 14px; outline: none; font-family: inherit; }
.form-grid textarea { resize: vertical; line-height: 1.5; }
.form-grid input:focus, .form-grid select:focus, .form-grid textarea:focus { border-color: #409EFF; }
.admin-primary-btn { margin-top: 18px; padding: 10px 18px; border: none; border-radius: 4px; background: #409EFF; color: #fff; cursor: pointer; font-weight: bold; }
.user-admin-list { display: flex; flex-direction: column; gap: 10px; }
.user-admin-row { display: grid; grid-template-columns: minmax(0, 1fr) 96px 80px; gap: 10px; align-items: center; padding: 12px; border: 1px solid #ebeef5; border-radius: 6px; background: #fff; }
.user-admin-row.current { background: #ecf5ff; border-color: #b3d8ff; }
.user-admin-meta { min-width: 0; display: flex; flex-direction: column; gap: 5px; }
.user-admin-meta strong { color: #303133; font-size: 15px; }
.user-admin-meta span { display: inline-block; margin-left: 8px; color: #409EFF; font-size: 12px; font-weight: bold; }
.user-admin-meta small { color: #909399; font-size: 12px; }
.user-admin-row .admin-secondary-btn, .user-admin-row .admin-danger-btn { min-height: 34px; }
.password-reset-box { margin-top: 18px; padding: 16px; border: 1px solid #ebeef5; border-radius: 6px; background: #fafafa; }
.password-reset-box h4 { margin: 0 0 14px; color: #303133; }
.password-actions { display: flex; gap: 10px; align-items: center; }
.password-actions .admin-secondary-btn { margin-top: 18px; padding: 10px 18px; }
.weight-editor { margin-top: 18px; border: 1px solid #ebeef5; border-radius: 6px; padding: 14px; }
.weight-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.weight-title h4 { margin: 0; color: #303133; }
.weight-title button, .remove-link-btn { border: none; border-radius: 4px; padding: 7px 10px; cursor: pointer; color: #fff; font-weight: bold; }
.weight-title button { background: #67C23A; }
.remove-link-btn { background: #F56C6C; }
.weight-row { display: grid; grid-template-columns: minmax(0, 1fr) 110px 24px 80px; gap: 10px; align-items: center; margin-bottom: 10px; }
.weight-row select, .weight-row input { padding: 9px 10px; border: 1px solid #dcdfe6; border-radius: 4px; }
.header { background-color: #fff; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); padding: 0 20px; display: flex; justify-content: space-between; align-items: center; z-index: 10; height: 60px; flex-shrink: 0; }
.header-btn { padding: 8px 16px; font-size: 14px; background-color: #f5f7fa; border: 1px solid #dcdfe6; border-radius: 6px; cursor: pointer; color: #606266; font-weight: bold; transition: all 0.3s; margin-left: 10px;}
.header-btn:hover { border-color: #409EFF; color: #409EFF; background-color: #ecf5ff; }
.header-select { padding: 8px 14px; min-width: 190px; font-size: 14px; background-color: #fff; border: 1px solid #dcdfe6; border-radius: 6px; cursor: pointer; color: #303133; font-weight: bold; outline: none; }
.header-select:focus { border-color: #409EFF; box-shadow: 0 0 0 2px rgba(64,158,255,0.12); }
.status-tag { background-color: #67C23A; color: #fff; padding: 5px 10px; border-radius: 4px; font-size: 14px; }

.main-layout { display: flex; height: calc(100vh - 60px); }
.sidebar { width: 160px; background-color: #2c3e50; color: #bfcbd9; display: flex; flex-direction: column; padding-top: 20px; flex-shrink: 0; }
.menu-item { padding: 15px 20px; cursor: pointer; font-size: 14px; transition: all 0.3s; border-left: 4px solid transparent; }
.menu-item:hover { background-color: #1f2d3d; color: #fff; }
.menu-item.active { background-color: #1f2d3d; color: #409EFF; border-left: 4px solid #409EFF; font-weight: bold; }
.content-area { flex-grow: 1; padding: 20px; overflow: hidden; background-color: #f5f7fa; }

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
.graph-loading { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.72); color: #909399; font-size: 16px; font-weight: bold; z-index: 90; }
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
.knowledge-video { margin: -6px 0 20px; }
.knowledge-video a { display: inline-block; padding: 8px 14px; background: #409EFF; color: #fff; border-radius: 4px; text-decoration: none; font-weight: bold; }
.knowledge-video a:hover { background: #337ecc; }
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
.detail-view { height: 100%; overflow-y: auto; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.08); padding: 20px; box-sizing: border-box; }
.detail-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; border-bottom: 2px solid #ebeef5; padding-bottom: 12px; margin-bottom: 18px; }
.detail-title { flex: 1; display: flex; align-items: center; gap: 10px; min-width: 0; }
.detail-title h3 { margin: 0; color: #303133; overflow-wrap: anywhere; }
.back-btn, .locate-btn { padding: 8px 14px; border: none; border-radius: 4px; color: #fff; cursor: pointer; font-weight: bold; }
.back-btn { background: #909399; }
.locate-btn { background: #409EFF; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; margin-top: 20px; }
.info-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05); transition: transform 0.2s; border-top: 4px solid #ddd; cursor: pointer; }
.info-card:hover { transform: translateY(-5px); box-shadow: 0 4px 16px 0 rgba(0,0,0,0.1); }
.ex-card { border-top-color: #F56C6C; display: flex; flex-direction: column; justify-content: space-between;}
.knowledge-card { border-top-color: #409EFF; display: flex; flex-direction: column; justify-content: space-between; }
.knowledge-card.active { border: 2px solid #409EFF; border-top-width: 4px; background: #ecf5ff; box-shadow: 0 4px 18px rgba(64,158,255,0.22); }
.other-card { border-top-color: #909399; display: flex; flex-direction: column; justify-content: space-between; }
.card-title { font-size: 16px; font-weight: bold; color: #333; margin-bottom: 10px; line-height: 1.4; }
.start-btn { margin-top: 15px; padding: 8px; background-color: #F56C6C; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
.knowledge-btn { margin-top: 15px; padding: 8px; background-color: #409EFF; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
.other-btn { margin-top: 15px; padding: 8px; background-color: #909399; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
</style>
