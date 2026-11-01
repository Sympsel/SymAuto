# 运行

## 三个都加载
```shell
.\gradlew.bat :dev:runClient
```

## 只加载公共前置
```shell
.\gradlew.bat :dev:runClient "-Pload=symbase"
```

## 加载公共前置 + HUD
```shell
.\gradlew.bat :dev:runClient "-Pload=symhud"
```

## 加载公共前置 + 自动化
```shell
.\gradlew.bat :dev:runClient "-Pload=symauto"
```

## 加载 HUD + 自动化
```shell
.\gradlew.bat :dev:runClient "-Pload=symhud,symauto"
```
