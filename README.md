# 阿里云DDNS
可用于动态更新域名解析地址

### 用法
java -jar aliyun-ddns.jar -DaccessKeyId=xxx -Dsecret=xxx -Ddomain=xxx

### 参数
#### 1、必填参数
1、accessKeyId 调用阿里云接口所需的key，该key需要有AliyunDNSFullAccess权限，即管理云解析（DNS）的权限
2、secret accessKeyId对应的秘钥
3、domain 要解析的域名，该域名需要已经在阿里云上有解析记录

#### 2、可选参数
1、regionId 地区信息, 默认: cn-hangzhou, 理论上域名解析不需要这个参数。
2、getIPUrl 用于获取本机公网ip的url, 默认: http://ifconfig.me, user-agent: curl/7.65.3 提供的url需要能通过curl直接获取ip