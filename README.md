Apollo（配置中心）
================

# 个人博客

[http://www.iocoder.cn](http://www.iocoder.cn/?github)

-------

![](http://www.iocoder.cn/images/common/wechat_mp.jpeg)

> 🙂🙂🙂关注**微信公众号：【芋艿的后端小屋】**有福利：  
> 1. RocketMQ / MyCAT / Sharding-JDBC **所有**源码分析文章列表  
> 2. RocketMQ / MyCAT / Sharding-JDBC **中文注释源码 GitHub 地址**  
> 3. 您对于源码的疑问每条留言**都**将得到**认真**回复。**甚至不知道如何读源码也可以请教噢**。  
> 4. **新的**源码解析文章**实时**收到通知。**每周更新一篇左右**。

-------

* 知识星球：![知识星球](http://www.iocoder.cn/images/Architecture/2017_12_29/01.png)

* 配置中心 **Apollo**

    * [《Apollo 源码解析 —— 调试环境搭建》](http://www.iocoder.cn/Apollo/build-debugging-environment?github&1611)
    * [《Apollo 源码解析 —— Portal 创建 App》](http://www.iocoder.cn/Apollo/portal-create-app?github&1611)
    * [《Apollo 源码解析 —— Portal 创建 Cluster》](http://www.iocoder.cn/Apollo/portal-create-cluster?github&1611)
    * [《Apollo 源码解析 —— Portal 创建 Namespace》](http://www.iocoder.cn/Apollo/portal-create-namespace?github&1611)
    * [《Apollo 源码解析 —— Portal 关联 Namespace》](http://www.iocoder.cn/Apollo/portal-associate-namespace?github&1611)
    * [《Apollo 源码解析 —— Portal 创建 Item》](http://www.iocoder.cn/Apollo/portal-create-item?github&1611)
    * [《Apollo 源码解析 —— Portal 批量变更 Item》](http://www.iocoder.cn/Apollo/portal-update-item-set?github&1611)
    * [《Apollo 源码解析 —— Admin Service 锁定 Namespace》](http://www.iocoder.cn/Apollo/admin-service-lock-namespace?github&1611)
    * [《Apollo 源码解析 —— Portal 发布配置》](http://www.iocoder.cn/Apollo/portal-publish?github&1611)
    * [《Apollo 源码解析 —— Admin Service 发送 ReleaseMessage》](http://www.iocoder.cn/Apollo/admin-server-send-release-message?github&1611)
    * [《Apollo 源码解析 —— Config Service 通知配置变化》](http://www.iocoder.cn/Apollo/config-service-notifications?github&1611)
    * [《Apollo 源码解析 —— Config Service 配置读取接口》](http://www.iocoder.cn/Apollo/config-service-config-query-api?github&1611)
    * [《Apollo 源码解析 —— Client 轮询配置》](http://www.iocoder.cn/Apollo/client-polling-config?github&1611)
    * [《Apollo 源码解析 —— Config Service 记录 Instance》](http://www.iocoder.cn/Apollo/config-service-audit-instance?github&1611)
    * [《Apollo 源码解析 —— Portal 创建灰度》](http://www.iocoder.cn/Apollo/portal-create-namespace-branch?github&1611)
    * [《Apollo 源码解析 —— Portal 配置灰度规则》](http://www.iocoder.cn/Apollo/portal-modify-namespace-branch-gray-rules?github&1611)
    * [《Apollo 源码解析 —— Portal 灰度发布》](http://www.iocoder.cn/Apollo/portal-publish-namespace-branch?github&1611)
    * [《Apollo 源码解析 —— Portal 灰度全量发布》](http://www.iocoder.cn/Apollo/portal-publish-namespace-branch-to-master?github&1611)
    * [《Apollo 源码解析 —— 服务自身配置 ServerConfig》](http://www.iocoder.cn/Apollo/server-config?github&1611)
    * [《Apollo 源码解析 —— Config Service 操作审计日志 Audit》](http://www.iocoder.cn/Apollo/config-service-audit?github&1611)
    * [《Apollo 源码解析 —— Portal 认证与授权（一）之认证》](http://www.iocoder.cn/Apollo/portal-auth-1?github&1611)
    * [《Apollo 源码解析 —— Portal 认证与授权（二）之授权》](http://www.iocoder.cn/Apollo/portal-auth-2?github&1611)
    * [《Apollo 源码解析 —— OpenAPI 认证与授权（一）之认证》](http://www.iocoder.cn/Apollo/openapi-auth-1?github&1611)
    * [《Apollo 源码解析 —— OpenAPI 认证与授权（二）之授权》](http://www.iocoder.cn/Apollo/openapi-auth-2?github&1611)
    * [《Apollo 源码解析 —— 服务的注册与发现》](http://www.iocoder.cn/Apollo/service-register-discovery?github&1611)
    * [《Apollo 源码解析 —— 客户端 API 配置（一）之一览》](http://www.iocoder.cn/Apollo/client-config-api-1?github&1611)
    * [《Apollo 源码解析 —— 客户端 API 配置（二）之 Config》](http://www.iocoder.cn/Apollo/client-config-api-2?github&1611)
    * [《Apollo 源码解析 —— 客户端 API 配置（三）之 ConfigFile》](http://www.iocoder.cn/Apollo/client-config-api-3?github&1611)
    * [《Apollo 源码解析 —— 客户端 API 配置（四）之 ConfigRepository》](http://www.iocoder.cn/Apollo/client-config-api-4?github&1611)
    * [《Apollo 源码解析 —— 客户端配置 Spring 集成（一）之 XML 配置》](http://www.iocoder.cn/Apollo/client-config-spring-1?github&1611)
    * [《Apollo 源码解析 —— 客户端配置 Spring 集成（二）之注解配置》](http://www.iocoder.cn/Apollo/client-config-spring-2?github&1611)
    * [《Apollo 源码解析 —— 客户端配置 Spring 集成（三）之外部化配置》](http://www.iocoder.cn/Apollo/client-config-spring-3?github&1611)

[![Build Status](https://travis-ci.org/ctripcorp/apollo.svg?branch=master)](https://travis-ci.org/ctripcorp/apollo)
[![GitHub release](https://img.shields.io/github/release/ctripcorp/apollo.svg)](https://github.com/ctripcorp/apollo/releases)
[![Coverage Status](https://coveralls.io/repos/github/ctripcorp/apollo/badge.svg?branch=master)](https://coveralls.io/github/ctripcorp/apollo?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
<a href="https://scan.coverity.com/projects/ctripcorp-apollo">
  <img alt="Coverity Scan Build Status" src="https://img.shields.io/coverity/scan/8244.svg"/>
</a>
[![codecov.io](https://codecov.io/github/ctripcorp/apollo/coverage.svg?branch=master)](https://codecov.io/github/ctripcorp/apollo?branch=master)

Apollo（阿波罗）是携程框架部门研发的分布式配置中心，能够集中化管理应用不同环境、不同集群的配置，配置修改后能够实时推送到应用端，并且具备规范的权限、流程治理等特性，适用于微服务配置管理场景。

服务端基于Spring Boot和Spring Cloud开发，打包后可以直接运行，不需要额外安装Tomcat等应用容器。

Java客户端不依赖任何框架，能够运行于所有Java运行时环境，同时对Spring/Spring Boot环境也有较好的支持。

.Net客户端不依赖任何框架，能够运行于所有.Net运行时环境。

更多产品介绍参见[Apollo配置中心介绍](https://github.com/ctripcorp/apollo/wiki/Apollo%E9%85%8D%E7%BD%AE%E4%B8%AD%E5%BF%83%E4%BB%8B%E7%BB%8D)

本地快速部署请参见[Quick Start](https://github.com/ctripcorp/apollo/wiki/Quick-Start)

# Screenshots
![配置界面](https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/apollo-home-screenshot.png)

# Features
* **统一管理不同环境、不同集群的配置**
  * Apollo提供了一个统一界面集中式管理不同环境（environment）、不同集群（cluster）、不同命名空间（namespace）的配置。
  * 同一份代码部署在不同的集群，可以有不同的配置，比如zk的地址等
  * 通过命名空间（namespace）可以很方便的支持多个不同应用共享同一份配置，同时还允许应用对共享的配置进行覆盖

* **配置修改实时生效（热发布）**
  * 用户在Apollo修改完配置并发布后，客户端能实时（1秒）接收到最新的配置，并通知到应用程序。

* **版本发布管理**
  * 所有的配置发布都有版本概念，从而可以方便的支持配置的回滚。

* **灰度发布**
  * 支持配置的灰度发布，比如点了发布后，只对部分应用实例生效，等观察一段时间没问题后再推给所有应用实例。

* **权限管理、发布审核、操作审计**
  * 应用和配置的管理都有完善的权限管理机制，对配置的管理还分为了编辑和发布两个环节，从而减少人为的错误。
  * 所有的操作都有审计日志，可以方便的追踪问题。

* **客户端配置信息监控**
  * 可以方便的看到配置在被哪些实例使用

* **提供Java和.Net原生客户端**
  * 提供了Java和.Net的原生客户端，方便应用集成
  * 支持Spring Placeholder, Annotation和Spring Boot的ConfigurationProperties，方便应用使用（需要Spring 3.1.1+）
  * 同时提供了Http接口，非Java和.Net应用也可以方便的使用

* **提供开放平台API**
  * Apollo自身提供了比较完善的统一配置管理界面，支持多环境、多数据中心配置管理、权限、流程治理等特性。
  * 不过Apollo出于通用性考虑，对配置的修改不会做过多限制，只要符合基本的格式就能够保存。
  * 在我们的调研中发现，对于有些使用方，它们的配置可能会有比较复杂的格式，如xml, json，需要对格式做校验。
  * 还有一些使用方如DAL，不仅有特定的格式，而且对输入的值也需要进行校验后方可保存，如检查数据库、用户名和密码是否匹配。
  * 对于这类应用，Apollo支持应用方通过开放接口在Apollo进行配置的修改和发布，并且具备完善的授权和权限控制

* **部署简单**
  * 配置中心作为基础服务，可用性要求非常高，这就要求Apollo对外部依赖尽可能地少
  * 目前唯一的外部依赖是MySQL，所以部署非常简单，只要安装好Java和MySQL就可以让Apollo跑起来
  * Apollo还提供了打包脚本，一键就可以生成所有需要的安装包，并且支持自定义运行时参数

# Usage
  1. [Apollo使用指南](https://github.com/ctripcorp/apollo/wiki/Apollo%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97)
  2. [Java客户端使用指南](https://github.com/ctripcorp/apollo/wiki/Java%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97)
  3. [.Net客户端使用指南](https://github.com/ctripcorp/apollo/wiki/.Net%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97)
  4. [其它语言客户端接入指南](https://github.com/ctripcorp/apollo/wiki/%E5%85%B6%E5%AE%83%E8%AF%AD%E8%A8%80%E5%AE%A2%E6%88%B7%E7%AB%AF%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97)
  5. [Apollo开放平台接入指南](https://github.com/ctripcorp/apollo/wiki/Apollo%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0)

# Design
  * [Apollo配置中心设计](https://github.com/ctripcorp/apollo/wiki/Apollo%E9%85%8D%E7%BD%AE%E4%B8%AD%E5%BF%83%E8%AE%BE%E8%AE%A1)
  * [Apollo核心概念之“Namespace”](https://github.com/ctripcorp/apollo/wiki/Apollo%E6%A0%B8%E5%BF%83%E6%A6%82%E5%BF%B5%E4%B9%8B%E2%80%9CNamespace%E2%80%9D)

# Development
  * [Apollo开发指南](https://github.com/ctripcorp/apollo/wiki/Apollo%E5%BC%80%E5%8F%91%E6%8C%87%E5%8D%97)
  * Code Styles
    * [Eclipse Code Style](https://github.com/ctripcorp/apollo/blob/master/apollo-buildtools/style/eclipse-java-google-style.xml)
    * [Intellij Code Style](https://github.com/ctripcorp/apollo/blob/master/apollo-buildtools/style/intellij-java-google-style.xml)

# Deployment
  * [Quick Start](https://github.com/ctripcorp/apollo/wiki/Quick-Start)
  * [分布式部署指南](https://github.com/ctripcorp/apollo/wiki/%E5%88%86%E5%B8%83%E5%BC%8F%E9%83%A8%E7%BD%B2%E6%8C%87%E5%8D%97)

# FAQ
  * [常见问题回答](https://github.com/ctripcorp/apollo/wiki/FAQ)
  * [部署&开发遇到的常见问题](https://github.com/ctripcorp/apollo/wiki/%E9%83%A8%E7%BD%B2&%E5%BC%80%E5%8F%91%E9%81%87%E5%88%B0%E7%9A%84%E5%B8%B8%E8%A7%81%E9%97%AE%E9%A2%98)

# Presentation
  * [携程开源配置中心Apollo的设计与实现](http://www.itdks.com/dakalive/detail/3420)
  * [Slides](http://techshow.ctrip.com/wp-content/uploads/2017/08/%E5%BC%80%E6%BA%90%E9%85%8D%E7%BD%AE%E4%B8%AD%E5%BF%83Apollo%E7%9A%84%E8%AE%BE%E8%AE%A1%E4%B8%8E%E5%AE%9E%E7%8E%B0-%E6%90%BA%E7%A8%8B%E5%AE%8B%E9%A1%BA.pdf)

# Publication
  * [开源配置中心Apollo的设计与实现](http://www.infoq.com/cn/articles/open-source-configuration-center-apollo)

# Support
![tech-support-qq](https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/tech-support-qq.png)

# Contribution
  * Source Code: https://github.com/ctripcorp/apollo
  * Issue Tracker: https://github.com/ctripcorp/apollo/issues

# License
The project is licensed under the [Apache 2 license](https://github.com/ctripcorp/apollo/blob/master/LICENSE).

# Known Users

> 按照登记顺序排序，更多接入公司，欢迎在[https://github.com/ctripcorp/apollo/issues/451](https://github.com/ctripcorp/apollo/issues/451)登记（仅供开源用户参考）

![携程](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/ctrip.png) 
![青石证券](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/bluestone.png) 
![沙绿](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/sagreen.png) 
![航旅纵横](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/umetrip.jpg) 
![58转转](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/zhuanzhuan.png) 
![蜂助手](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/phone580.png) 
![海南航空](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/hainan-airlines.png) 
![CVTE](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/cvte.png) 
![明博教育](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/mainbo.jpg) 
![麻袋理财](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/madailicai.png) 
![美行科技](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/mxnavi.jpg) 
![首展科技](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/fshows.jpg) 
![易微行](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/feezu.png) 
![人才加](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/rencaijia.png) 
![凯京集团](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/keking.png) 
![乐刻运动](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/leoao.png) 
![大疆](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/dji.png) 
![快看漫画](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/kkmh.png) 
![我来贷](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/wolaidai.png) 
![虚实软件](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/xsrj.png) 
![网易严选](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/yanxuan.png) 
![视觉中国](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/sjzg.png) 
![资产360](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/zc360.png) 
![亿咖通](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/ecarx.png) 
![5173](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/5173.png) 
![沪江](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/hujiang.png) 
![网易云基础服务](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/163yun.png) 
![现金巴士](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/cash-bus.png) 
![锤子科技](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/smartisan.png) 
![头等仓](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/toodc.png) 
![吉祥航空](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/juneyaoair.png) 
![263移动通信](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/263mobile.png) 
![投投金融](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/toutoujinrong.png) 
![每天健康](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/mytijian.png) 
![麦芽金服](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/maiyabank.png) 
![蜂向科技](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/fengunion.png) 
![即科金融](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/geex-logo.png) 
![贝壳网](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/beike.png) 
![有赞](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/youzan.png) 
![云集汇通](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/yunjihuitong.png) 
![犀牛瀚海科技](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/rhinotech.png) 
![农信互联](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/nxin.png) 
![蘑菇租房](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/mgzf.png) 
![狐狸金服](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/huli-logo.png) 
![漫道集团](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/mandao.png) 
![怪兽充电](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/enmonster.png) 
![南瓜租房](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/nanguazufang.png) 
![石投金融](https://github.com/ctripcorp/apollo/blob/master/doc/images/known-users/shitoujinrong.png) 
