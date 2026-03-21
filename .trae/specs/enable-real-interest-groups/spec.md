# Tasks

* [ ] Task 1: 完成消息通知中心 MVP 方案设计

  * [ ] SubTask 1.1: 定义通知事件模型（私聊新消息、@提及我）

  * [ ] SubTask 1.2: 设计通知列表、未读计数与已读流转

  * [ ] SubTask 1.3: 明确前后端接口与落库字段

* [ ] Task 2: 完成私聊会话 MVP 方案设计

  * [ ] SubTask 2.1: 设计会话创建/复用与会话列表结构

  * [ ] SubTask 2.2: 设计私聊消息发送、接收、历史查询链路

  * [ ] SubTask 2.3: 定义私聊窗口核心交互状态

* [ ] Task 3: 完成聊天室连续性与实时状态 MVP 方案设计

  * [ ] SubTask 3.1: 设计断线补拉与未读计数规则

  * [ ] SubTask 3.2: 设计历史消息条数配置化读取路径

  * [ ] SubTask 3.3: 设计在线用户列表与正在输入状态同步机制

* [ ] Task 4: 完成全站搜索入口与结果页方案设计

  * [ ] SubTask 4.1: 设计导航搜索入口与结果页信息结构

  * [ ] SubTask 4.2: 设计排序切换（相关度、时间）规则

  * [ ] SubTask 4.3: 明确与现有吧内搜索的关系与迁移策略

* [ ] Task 5: 输出第二轮实施优先级与验收指标

  * [ ] SubTask 5.1: 形成 P0/P1/P2 上线节奏（聊天链路优先）

  * [ ] SubTask 5.2: 定义每项功能的可验收指标

  * [ ] SubTask 5.3: 标注风险、依赖与回滚策略

# Task Dependencies

* Task 2 depends on Task 1

* Task 3 depends on Task 2

* Task 4 depends on Task 3

* Task 5 depends on Task 2

* Task 5 depends on Task 3

* Task 5 depends on Task 4

