# anchorDNS ![adnsLogo][1]

**anchorDNS** 是一个运行在本地的分流DNS服务器

#这能干啥？

众所周知的 [CHNRoutes][2] 项目能够修改系统路由表使拨VPN后 中国IP依然直接连接，仅国外连接通过VPN以防止国内网站也通过VPN绕行。然而为了解决DNS污染，一般会让DNS解析全部走VPN到国外DNS服务器，因此上了CDN或有国外服务器的中国网站 依然会解析到国外或国内非最佳服务器降低访问速度。

anchorDNS 正是为解决此问题而设计，通过先从国内DNS解析域名，若发现是中国IP则直接返回结果，若非中国IP则改用境外DNS解析的方法 能够既完全保持国内网站的解析正确 又获得无污染的境外网站DNS查询结果。

示例——

解析 www.baidu.com -> 同时查询114.114.114.114(直连)/8.8.8.8(走VPN) -> 发现是中国IP网站 -> 使用直连114.114.114.114的结果

解析 twitter.com -> 同时查询114.114.114.114(直连)/8.8.8.8(走VPN) -> 发现非中国IP网站 -> 使用走VPN的8.8.8.8的结果（无污染）

#使用姿势

去 Release 下载最新版，解压后bin目录下 带参数运行 anchorDNS

参数详解——

    usage: anchorDNS
     -a,--alternativeDNS <arg>   Specify the alternative DNS server. Default:
                                 8.8.8.8
     -c,--cidr <arg>             Specify the CIDR list. Default: ChinaCIDR.txt
     -d,--defaultDNS <arg>       Specify the default DNS server. Default:
                                 114.114.114.114
     -f,--fallback               Use alternative DNS when default DNS failed.
     -h,--help                   Show this help message.
     -i,--ip <arg>               Specify the listening IP. Default: 127.0.0.1
     -n,--nocache                Disable results cache.
     -p,--port <arg>             Specify the listening port. Default: 53
     -t,--timeout <arg>          Specify the DNS time out (sec). Default: 2

    -a 指定国外DNS服务器
    -c 指定中国IP CIDR表（默认用本项目下的 ChinaCIDR.txt ）
    -d 指定国内DNS服务器
    -f 当国内DNS超时或出错时也转至国外DNS
    -h 显示上述帮助
    -i 指定监听IP（本地请使用默认127.0.0.1）
    -n 禁用查询缓存
    -p 指定监听端口
    -t DNS查询超时时间 单位：秒 默认为2

可以直接不带任何参数使用默认值使用，也可进行自定义如： `anchorDNS -d 1.2.4.8 -a 8.8.4.4 -f -c ChinaCIDR_NEW.txt`

**然后设置机器的DNS服务器为 127.0.0.1**

中国IP段的CIDR表可在[此处下载更新][3]

#技术细节

使用 Netty 5.0 / dnsjava 处理DNS通信，Gradle构建，JDK1.6+

  [1]: logo.png "anchorDNS"
  [2]: https://github.com/fivesheep/chnroutes
  [3]: http://www.ipdeny.com/ipblocks/data/aggregated/cn-aggregated.zone