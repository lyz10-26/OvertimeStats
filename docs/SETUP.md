# 可选企业同步服务

通知采集不需要本服务。仅当企业已批准考勤查询权限时使用。

1. 企业管理员创建钉钉企业内部应用，开通“查询企业考勤数据”权限，配置本人所在的应用可见及授权范围。
2. 按 `backend/.env.example` 在服务端设置 `DING_APP_KEY`、`DING_APP_SECRET`、`DING_USER_ID` 和至少32字符的随机 `BRIDGE_TOKEN`。
3. 使用Python 3.10+执行 `python3 backend/server.py`。服务只监听 `127.0.0.1:8787`。
4. 用企业HTTPS反向代理暴露 `/attendance`，转发Authorization请求头；在App中填写HTTPS根地址与BRIDGE_TOKEN。

仅支持服务端绑定的一个员工，客户端不能查询其他员工。请求为 `GET /attendance?start=YYYY-MM-DD&end=YYYY-MM-DD`，使用 `Authorization: Bearer <BRIDGE_TOKEN>`。

服务调用钉钉 `/attendance/list`，按7天分段分页，过滤NotSigned占位记录，多班次冲突拒绝自动合并。默认最近180天、单次最多31天。实际可用范围以企业授权及钉钉接口为准。

默认周一至周五为工作日，不读取企业假期审批或国家调休安排；特殊日期需在App里人工调整。不要把企业AppSecret或个人访问令牌写入源码。
