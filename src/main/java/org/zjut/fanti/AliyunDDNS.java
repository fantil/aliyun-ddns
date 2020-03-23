package org.zjut.fanti;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.utils.StringUtils;
import com.google.gson.Gson;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * aliyun DDNS
 */
public class AliyunDDNS {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDDNS.class);

    private static final String DEFAULT_GET_IP_URL = "http://ifconfig.me";

    private static IAcsClient client;

    public static void main(String[] args) throws Exception {
        String domain = System.getProperty("domain");
        String regionId = System.getProperty("regionId");
        String accessKeyId = System.getProperty("accessKeyId");
        String secret = System.getProperty("secret");
        String getIPUrl = System.getProperty("getIPUrl");
        if (StringUtils.isEmpty(domain)) {
            LOGGER.error("未设置要解析的domain, 请检查参数");
            return;
        }

        if (StringUtils.isEmpty(accessKeyId)) {
            LOGGER.error("未设置要解析的accessKeyId, 无法请求阿里云api");
            return;
        }

        if (StringUtils.isEmpty(secret)) {
            LOGGER.error("未设置secret, 无法请求阿里云api");
            return;
        }

        if (StringUtils.isEmpty(regionId)) {
            regionId = "cn-hangzhou";
        }

        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, secret);
        client = new DefaultAcsClient(profile);

        String ip = getMyIP(getIPUrl);
        if (StringUtils.isEmpty(ip)) {
            LOGGER.error("未获取到本机公网ip, 不做解析");
        }

        DescribeDomainRecordsResponse.Record record = getDomainRecord(domain);
        if (record == null) {
            LOGGER.error("根据域名获取解析记录失败, 请检查域名或key是否正确");
            return;
        }

        updateDNSInfo(ip, record);
    }

    /**
     * 更新阿里云dns解析
     *
     * @param newIP  新的ip
     * @param record 要更新的记录
     */
    private static void updateDNSInfo(String newIP, DescribeDomainRecordsResponse.Record record) {
        if (newIP.equals(record.getValue())) {
            LOGGER.info("当前解析记录与本机ip一致, 不再重复解析");
            return;
        }
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setRecordId(record.getRecordId());
        request.setRR(record.getRR());
        request.setType("A");
        request.setValue(newIP);

        try {
            UpdateDomainRecordResponse response = client.getAcsResponse(request);

            if (!StringUtils.isEmpty(response.getRecordId())) {
                LOGGER.info("解析成功, 新记录对应的ip: {}, 解析记录: {}", newIP, new Gson().toJson(response));
            }
        } catch (ServerException e) {
            LOGGER.error(e.getErrMsg(), e);
        } catch (ClientException e) {
            LOGGER.error("ErrCode: {}", e.getErrCode());
            LOGGER.error("ErrMsg: {}", e.getErrMsg());
            LOGGER.error("RequestId: {}", e.getRequestId());
        }
    }

    /**
     * 根据域名获取相应的信息
     *
     * @param domain 3级以上域名, 如: pan.baidu.com
     * @return
     */
    private static DescribeDomainRecordsResponse.Record getDomainRecord(String domain) {
        if (!domain.contains(".")) {
            return null;
        }
        //pan.baidu
        String prefixDomain = domain.substring(0, domain.lastIndexOf("."));
        if (!prefixDomain.contains(".")) {
            return null;
        }
        //pan
        String prefixSubDomain = prefixDomain.substring(0, prefixDomain.lastIndexOf("."));

        //子域名, 例如: baidu.com
        String subDomain = domain.substring(prefixSubDomain.length() + 1);
        if (StringUtils.isEmpty(subDomain) || StringUtils.isEmpty(prefixSubDomain) || StringUtils.isEmpty(prefixDomain)) {
            return null;
        }

        LOGGER.info("根域名: {}", subDomain);
        DescribeDomainRecordsRequest request = new DescribeDomainRecordsRequest();
        request.setDomainName(subDomain);

        try {
            DescribeDomainRecordsResponse response = client.getAcsResponse(request);
            if (response.getTotalCount() == 0) {
                return null;
            }

            for (DescribeDomainRecordsResponse.Record record : response.getDomainRecords()) {
                if (prefixSubDomain.equals(record.getRR())) {
                    LOGGER.info("该域名当前解析记录: {}", new Gson().toJson(record));
                    return record;
                }
            }
            LOGGER.info("未找到该域名对应的解析记录: {}", new Gson().toJson(response));
        } catch (ServerException e) {
            LOGGER.error(e.getErrMsg(), e);
        } catch (ClientException e) {
            LOGGER.error("ErrCode: {}", e.getErrCode());
            LOGGER.error("ErrMsg: {}", e.getErrMsg());
            LOGGER.error("RequestId: {}", e.getRequestId());
        }

        return null;
    }

    /**
     * 获取本机公网ip
     *
     * @param getIPUrl 设置从哪个网站获取
     * @return 公网ip地址, 未获取到则为""
     */
    private static String getMyIP(String getIPUrl) {
        if (StringUtils.isEmpty(getIPUrl)) {
            getIPUrl = DEFAULT_GET_IP_URL;
        }
        Headers headers = new Headers.Builder().add("user-agent", "curl/7.65.3").build();
        Request request = new Request.Builder().headers(headers).url(getIPUrl).build();
        OkHttpClient client = new OkHttpClient();

        String result = "";
        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            result = response.body().string();
            LOGGER.info("本机公网ip: {}", result);
        } catch (Exception e) {
            LOGGER.error("error info:" + e.getMessage());
        }

        return result;
    }
}
