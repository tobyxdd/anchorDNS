# anchorDNS ![adnsLogo][1]

**anchorDNS** 是一个运行在本地的分流DNS服务器

#这能干啥？

众所周知的 [CHNRoutes][2] 项目能够修改系统路由表使拨VPN后 中国IP依然直接连接，仅国外连接通过VPN以防止国内网站也通过VPN绕行。然而为了解决DNS污染，一般会让DNS解析全部走VPN到国外DNS服务器，因此上了CDN或有国外服务器的中国网站 依然会解析到国外或国内非最佳服务器降低访问速度。

anchorDNS 正是为解决此问题而设计，通过先从国内DNS解析域名，若发现是中国IP则直接返回结果，若非中国IP则改用境外DNS解析的方法 能够既完全保持国内网站的解析正确 又获得无污染的境外网站DNS查询结果。**用户只需分别提供一个自己喜欢的国内DNS与国外DNS地址，与一个中国IP段的CIDR表（已自带）**

*另附"reverse"反模式，解释见下文*

示例——
解析 www.baidu.com -> 查询114.114.114.114 -> 发现是中国IP -> 直接使用
解析 twitter.com -> 查询114.114.114.114 -> 发现非中国IP -> 抛弃结果查询8.8.8.8 -> 使用正确无污染结果

#使用姿势

去 Release 下载最新版，解压后bin目录下 带参数运行 anchorDNS

参数详解——

    usage: anchorDNS
     -a,--alternativeDNS <arg>   Specify the alternative DNS server.
     -c,--cidr <arg>             Specify the CIDR list.
     -d,--defaultDNS <arg>       Specify the default DNS server.
     -h,--help                   Show this help message.
     -i,--ip <arg>               Specify the listening IP. Default: 127.0.0.1
     -n,--nocache                Disable results cache.
     -p,--port <arg>             Specify the listening port. Default: 53
     -r,--reverse                Check the alternative DNS first.
     -t,--timeout <arg>          Specify the DNS time out (sec). Default: 2

    -a 指定国外DNS服务器
    -c 指定中国IP CIDR表（可用本项目下的 **ChinaCIDR.txt** 或 [此处下载最新版][3]）
    -d 指定国内DNS服务器
    -i 指定监听IP（本地请使用默认127.0.0.1）
    -n 禁用查询缓存
    -p 指定监听端口
    -r 反模式：优先查询国外DNS 发现中国IP后再改用国内DNS **（不建议使用！）**
    -t DNS查询超时时间 单位：秒 默认为2

比如： `anchorDNS -d 114.114.114.114 -a 8.8.8.8 -c ChinaCIDR.txt`

**然后设置机器的DNS服务器为 127.0.0.1 ！**

#技术细节

使用 Netty 5.0 / dnsjava 处理DNS通信，Gradle构建，JDK1.6+

  [1]: logo.png "anchorDNS"
  [2]: https://github.com/fivesheep/chnroutes
  [3]: http://www.ipdeny.com/ipblocks/data/aggregated/cn-aggregated.zone