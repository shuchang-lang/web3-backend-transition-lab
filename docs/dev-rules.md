# Dev Rules

## 固定约束

- 不允许使用内存数据库，数据库固定为 `MySQL`。
- ORM / 持久层框架固定为 `MyBatis-Plus`。
- 实体对象默认使用 `lombok.Data`。
- 对外接口说明优先采用 `SpringDoc + Javadoc`，尽量保持零注解侵入。

## 注释规范

- 所有实体字段必须逐个写中文 Javadoc。
- 对外暴露的接口必须写中文 Javadoc。
- 涉及 Web3 调用的关键逻辑必须写中文注释。
- 新增或修改的注释默认全部使用中文。

## 开发方式

- 继续某一天时，优先基于：
  - `docs/current-status.md`
  - `docs/day-handoff.md`
- 默认不要整仓全量重读，只读本次必要文件。
- 做代码改动后，默认运行 `practice/web3-wallet-backend-demo` 下的 `mvn test`。
- 完成阶段任务后，优先同步：
  - `docs/current-status.md`
  - `docs/day-handoff.md`

## 文档同步规则

- 当某一天形成稳定成果时，同步：
  - 对应 `docs/dayXX-*.md`
  - `README.md`
  - `practice/web3-wallet-backend-demo/README.md`
  - `roadmap/next-steps.md`

## Git 与编辑原则

- 不回退用户已有未提交改动。
- 不擅自更换既定技术栈。
- 不为了“省事”绕过中文注释和文档同步。
