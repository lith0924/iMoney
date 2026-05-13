# iMoney Server

本仓库为 **iMoney** 记账**服务端**（Spring Boot），通过 API 与 **微信 iLink** 等与客户端、机器人侧对接。

---

## 功能介绍

iMoney 希望把**微信聊天窗口**变成随身记账入口：不必专门打开记账 App，像发消息一样完成记录与查询（**想到就记，发一句就记**）。  
以下从**使用者视角**归纳产品侧重的能力；菜单演示、分功能截图与更细的指令说明可在 Wiki 等文档中后续补充。

| 方向 | 说明 |
|------|------|
| **聊天式快捷记账** | 在微信里直接发结构化短句即可记账，例如 `+100 工资`、`-18 午饭`、`-32 打车`，减少专门打开记账 App 的成本。 |
| **口语化 / 自然语言** | 支持更接近日常说话的表达（如「今天中午吃饭花了 28」），在启用大模型解析时识别效果更稳；未启用时以规则与指令为主。 |
| **语音一键记账** | 支持发送语音消息，完成识别与解析后自动入账，无需手打文字，适合边走边记、不便打字等场景。 |
| **微信即入口** | 以高频使用的聊天窗口为入口，缩短「想记 → 完成记」的路径，便于坚持。 |
| **轻量协作记账** | 家庭、情侣、小团队、室友等「一起记」场景：共享圈子与账本，多人协同、账目集中。 |
| **预算** | 按日/月/年等周期关注额度与支出节奏，支撑预算类查询与后续预警类能力扩展。 |
| **分析与复盘** | 按时间、分类、圈子等维度汇总收支，支撑「钱花在哪、结构是否合理」类查询。 |
| **导出** | 支持账单导出（如 Excel），便于存档、家庭整理或二次分析。 |

连接微信侧时，可发送 **「菜单」** 查看能力入口，发送 **「帮助」** 查看指令说明（具体以当前版本实现为准）。iLink 相关能力可参考 [wechat-ilink-sdk-java](https://github.com/lith0924/wechat-ilink-sdk-java)。完成部署后，通过下文 **「快速部署」** 中的地址进入扫码页，即可连接微信侧。

---

## 后续规划

| 方向 | 说明 |
|------|------|
| **多部署形态** | 在现有可执行制品部署方式之外，补充容器化与编排示例，便于在不同运维环境中落地。 |
| **AI 能力与模型抽象** | 评估引入 **Spring AI** 等统一抽象，支持**多模型、多厂商**可选与路由；通过提示词与工具调用规范化，提升**指令识别准确率**与可维护性。 |
| **图表与统计** | 提供按时间、分类、圈子、预算执行等维度的**聚合统计与指标接口**，便于前端或报表侧**直观预览**趋势与占比。 |
| **指令体系细化** | 扩展**动词、参数、校验与帮助说明**，覆盖更多记账、查询与协作操作；在演进中兼顾对已有客户端的兼容策略。 |

上述内容不构成交付承诺；排期以 Issue 与里程碑为准。欢迎通过 Issue / Pull Request 提出建议与实现。

---

## 快速部署

### 1. 数据库

在目标库中创建数据库（名称与环境中使用的库名一致即可，示例为 `imoney`）：

```sql
CREATE DATABASE imoney DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

在同一库中执行 **`doc/sql/imoney-schema.sql`**，创建 iMoney 所需的业务表（含预算等）。

### 2. 环境变量

**MySQL（`dev` 有本地默认值；`prod` 必须设置，勿写进仓库）**

| 变量 | 说明 |
|------|------|
| `MYSQL_URL` | 完整 JDBC 地址，例如 `jdbc:mysql://127.0.0.1:3306/imoney?useSSL=false&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&serverTimezone=GMT%2B8&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true`。生产环境**必填**。 |
| `MYSQL_USERNAME` | 数据库用户名；`dev` 默认 `root`。 |
| `MYSQL_PASSWORD` | 数据库密码；`dev` 默认为空，生产环境**必填**。 |

**可选：AI 记账辅助**（对应 `application.yml` 中的 `imai.zhipu` 等）

| 变量 | 说明 |
|------|------|
| `IMAI_ZHIPU_API_KEY` | 智谱等平台 API 密钥；未设置时，依赖大模型的解析能力不可用。 |
| `IMAI_ZHIPU_MODEL` | 可选；模型名称，未设置时使用配置中的默认值。 |

敏感信息通过环境变量或私密配置注入，**勿提交**到版本库。

### 3. 构建与启动

```bash
mvn -DskipTests package
java -jar target/blade-api-exe.jar
```

- HTTP 端口以 `application.yml` 中 `server.port` 为准（本地一般为 **8183**）。  
- 制品通常为 **`target/blade-api-exe.jar`**（以 `pom.xml` 中 `finalName` 与 `spring-boot-maven-plugin` 的 `classifier` 为准）。  
- 使用 **`prod` 配置**时须预先设置 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`（及 `REDIS_HOST` 等），例如：`java -jar -Dspring.profiles.active=prod target/blade-api-exe.jar`（具体以你方启动脚本为准）。

### 4. 连接微信（ilink 扫码）

服务启动后，在浏览器打开：**[http://localhost:8183/index.html](http://localhost:8183/index.html)**（若修改了端口，请替换 URL 中的端口）。  
进入内置 **ilink 扫码登录** 页：自动拉取二维码，也可点击「刷新二维码」。

<img width="453" height="606" alt="0a9f5650-6279-4ccb-8a3a-d633976f9415" src="https://github.com/user-attachments/assets/314cda60-6942-413c-b396-a47280ae51c5" />

### 5. 服务端模块说明（选读）

本仓库在实现上大致对应如下能力边界（以 `doc/sql` 与 `modules/business` 下代码为准）：

- **用户与身份**：业务用户、上下文（当前圈子等）相关数据与接口。  
- **圈子与协作**：共享圈子、成员与权限。  
- **记账与流水**：收入/支出及业务规则下的记账、查询与扩展。  
- **预算**：预算周期与额度（表结构见 `doc/sql/imoney-schema.sql`）。  
- **指令与入口**：命令路由、与 IM/机器人链路的对接（如文本消息触发业务逻辑）。  
- **可选 AI 增强**：自然语言解析与智能辅助；未启用时按产品设计降级或不可用。

---

## 社区群

欢迎加入 **iMoney 用户与开发者交流群**，用于日常答疑、版本通知与使用反馈（加群请注明「iMoney」以免误拒）。

| 渠道 | 说明 |
|------|------|
| **微信群** | <img width="280" alt="f48d94484637905f3b16ec54ca407dde" src="https://github.com/user-attachments/assets/1dc854a8-d3e9-476b-b31e-cde19573e54d" /> |

与 Roadmap、缺陷与功能建议相关的讨论，仍建议优先使用 **Issue / Pull Request**，便于检索与归档；社区群更适合即时交流与轻量问答。

---

## 参与贡献

- **代码与文档**：通过 GitHub 提交 Pull Request，并附变更说明、影响范围与自测说明。  
- **需求与讨论**：通过 Issue 描述场景、期望行为或与 Roadmap 相关的想法；复杂方案可先讨论再实现。

感谢参与。
