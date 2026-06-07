# Trip Ledger Workbench

一个零依赖的学习前端。

目标不是复刻原来的小程序，而是把后端接口按业务域排开，方便你：

- 按模块理解整个项目
- 直接调试本地 Spring Boot 接口
- 在一个页面里完成登录、拿 token、填上下文、跑接口

## 启动

后端先保持运行在 `http://127.0.0.1:8080`。

然后在这个目录下起一个静态文件服务：

```bash
cd /Users/dora/Downloads/trip-ledger/fronted/ledger-workbench
python3 -m http.server 4173
```

浏览器打开：

[http://127.0.0.1:4173/](http://127.0.0.1:4173/)

## 学习建议

第一轮只走这条链路：

1. `System -> Ping`
2. `Auth -> List Mock Users`
3. `Auth -> Wechat Login`
4. `User -> Current User`
5. `Books -> Create Book`
6. `Books -> My Books`
7. `Books -> Book Detail`

等这条链路顺了，再继续看：

- `Members`
- `Invitations`
- `Categories`
- `Temp Participants`
- `Bills`
- `Bill Requests`
- `Settlements`
- `Payment Confirms`
- `Statistics`

## 备注

- `Files -> Upload File` 现在只留了接口位，没有在纯静态页里做 multipart 上传。
- 旧的 uni-app 前端源码已经挪到：
  - `/Users/dora/Downloads/trip-ledger/fronted/_legacy-ledger-fronted`
