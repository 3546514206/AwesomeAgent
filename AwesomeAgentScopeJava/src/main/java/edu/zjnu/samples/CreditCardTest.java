package edu.zjnu.samples;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CreditCardTest {

    public static void main(String[] args) {
        testDifferentJsonFormats();
    }

    /**
     * 测试不同的 JSON 转换方式
     */
    public static void testDifferentJsonFormats() {
        String defqSqjNo = "2602098864000001";
        InputInfo existInputInfo = buildExistInputInfo();
        BhData bhdata = buildBhData();

        log.info("========== 测试开始 ==========");

        // 测试方式1：直接转字符串
        testMethod1(defqSqjNo, existInputInfo, bhdata);

        // 测试方式2：使用 JSONObject 转字符串
        testMethod2(defqSqjNo, existInputInfo, bhdata);

        // 测试方式3：只传必要字段
        testMethod3(defqSqjNo, existInputInfo, bhdata);
    }

    /**
     * 方式1：直接使用 JSONUtil.toJsonStr()
     */
    private static void testMethod1(String defqSqjNo, InputInfo existInputInfo, BhData bhdata) {
        log.info("---------- 方式1：JSONUtil.toJsonStr() ----------");
        updateAndSend(defqSqjNo, existInputInfo, bhdata,
                info -> JSONUtil.toJsonStr(info));
    }

    /**
     * 方式2：使用 JSONObject 再 toString()
     */
    private static void testMethod2(String defqSqjNo, InputInfo existInputInfo, BhData bhdata) {
        log.info("---------- 方式2：JSONObject.toString() ----------");
        updateAndSend(defqSqjNo, existInputInfo, bhdata,
                info -> new JSONObject(info).toString());
    }

    /**
     * 方式3：只传下游需要的字段（推荐）
     */
    private static void testMethod3(String defqSqjNo, InputInfo existInputInfo, BhData bhdata) {
        log.info("---------- 方式3：只传必要字段 ----------");
        updateAndSend(defqSqjNo, existInputInfo, bhdata,
                info -> buildNeededJson(info).toString());
    }

    /**
     * 构建只包含必要字段的 JSON
     */
    private static JSONObject buildNeededJson(InputInfo info) {
        JSONObject json = new JSONObject();
        // 基础信息
        json.put("engname", info.getEngname());
        json.put("nationl", info.getNationl());
        json.put("date_of_filling", info.getDateOfFilling());
        json.put("mainsex", info.getMainsex());
        json.put("idno", info.getIdno());
        json.put("mobile", info.getMobile());
        json.put("mainname", info.getMainname());
        json.put("maindate", info.getMaindate());
        json.put("status", info.getStatus());
        json.put("risklevel", info.getRisklevel());
        json.put("sqxyLimit", info.getSqxyLimit());
        json.put("preCrdtLimit", info.getPreCrdtLimit());

        // 征信相关字段（更新后的）
        json.put("taxDaId", info.getTaxDaId());
        json.put("taxStatus", info.getTaxStatus());
        json.put("psbcDaId", info.getPsbcDaId());
        json.put("psbcStatus", info.getPsbcStatus());
        json.put("houseFundDaId", info.getHouseFundDaId());
        json.put("houseFundStatus", info.getHouseFundStatus());
        json.put("alipayDaId", info.getAlipayDaId());
        json.put("alipayStatus", info.getAlipayStatus());
        json.put("weChatDaId", info.getWeChatDaId());
        json.put("weChatStatus", info.getWeChatStatus());
        json.put("icbcDaId", info.getIcbcDaId());
        json.put("icbcStatus", info.getIcbcStatus());

        // 其他需要字段...
        return json;
    }

    /**
     * 通用的更新和发送方法
     */
    private static void updateAndSend(String defqSqjNo, InputInfo existInputInfo,
                                      BhData bhdata, InputInfoConverter converter) {
        // 更新字段
        existInputInfo.setTaxDaId(bhdata.getTaxDaId());
        existInputInfo.setTaxStatus(bhdata.getTaxStatus() != null ? String.valueOf(bhdata.getTaxStatus()) : null);
        existInputInfo.setPsbcDaId(bhdata.getPsbcDaId());
        existInputInfo.setPsbcStatus(bhdata.getPsbcStatus() != null ? String.valueOf(bhdata.getPsbcStatus()) : null);
        existInputInfo.setHouseFundDaId(bhdata.getHouseFundDaId());
        existInputInfo.setHouseFundStatus(bhdata.getHouseFundStatus() != null ? String.valueOf(bhdata.getHouseFundStatus()) : null);
        existInputInfo.setAlipayDaId(bhdata.getAlipayDaId());
        existInputInfo.setAlipayStatus(bhdata.getAlipayStatus() != null ? String.valueOf(bhdata.getAlipayStatus()) : null);
        existInputInfo.setWeChatDaId(bhdata.getWeChatDaId());
        existInputInfo.setWeChatStatus(bhdata.getWeChatStatus() != null ? String.valueOf(bhdata.getWeChatStatus()) : null);
        existInputInfo.setIcbcDaId(bhdata.getIcbcDaId());
        existInputInfo.setIcbcStatus(bhdata.getIcbcStatus() != null ? String.valueOf(bhdata.getIcbcStatus()) : null);

        // 使用转换器将 inputInfo 转为字符串
        SMHL0060Req smhl0060Req = new SMHL0060Req();
        SMHL0060ReqList applyInfo = new SMHL0060ReqList();
        applyInfo.setSqjNo(defqSqjNo);
        applyInfo.setInputScene("10");
        applyInfo.setInputFlag("126");
        applyInfo.setInputInfo(converter.convert(existInputInfo));  // 这里是 String

        List<SMHL0060ReqList> applyInfoList = new ArrayList<>();
        applyInfoList.add(applyInfo);
        smhl0060Req.setList(applyInfoList);

        // 打印最终请求
        String reqJson = JSONUtil.toJsonStr(smhl0060Req);
        log.info("最终请求JSON: {}", reqJson);
        System.out.println(reqJson);

        // 验证是否能正确解析
        verifyJsonParsable(reqJson);
        log.info("========================================\n");
    }

    /**
     * 验证 JSON 是否可解析
     */
    private static void verifyJsonParsable(String jsonStr) {
        try {
            // 先解析外层
            JSONObject root = JSONUtil.parseObj(jsonStr);
            JSONArray list = root.getJSONArray("list");
            if (list != null && !list.isEmpty()) {
                JSONObject first = list.getJSONObject(0);
                String inputInfoStr = first.getStr("inputInfo");

                // 再解析内层
                if (inputInfoStr != null) {
                    try {
                        JSONObject inner = JSONUtil.parseObj(inputInfoStr);
                        log.info("✅ inputInfo 解析成功！字段数: {}", inner.size());
                        log.info("   engname: {}", inner.getStr("engname"));
                        log.info("   idno: {}", inner.getStr("idno"));
                    } catch (Exception e) {
                        log.error("❌ inputInfo 解析失败: {}", e.getMessage());
                        log.error("   inputInfo 内容: {}", inputInfoStr);
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ 外层 JSON 解析失败: {}", e.getMessage());
        }
    }

    /**
     * 构建 existInputInfo（模拟数据库查询结果）
     */
    private static InputInfo buildExistInputInfo() {
        InputInfo inputInfo = new InputInfo();
        inputInfo.setEngname("ZAO SHUYANXIA");
        inputInfo.setNationl("01");
        inputInfo.setDateOfFilling("20260209");
        inputInfo.setMainsex("GEND-F");
        inputInfo.setWereAlsoIsPers("0");
        inputInfo.setDeviceId("o07uS6w0yeyvaTVg1OUlgMZLGRlk");
        inputInfo.setOpine("执行人行征信失败，未从行内征信系统获取到该客户信息");
        inputInfo.setTaxStatus("10");
        inputInfo.setZaCode("20000000");
        inputInfo.setRemoteInterviewFlag("0");
        inputInfo.setAudittype("SP-02");
        inputInfo.setFeetype("FEECD-C");
        inputInfo.setPromotermobile("13533438565");
        inputInfo.setSpreaderName("苗-");
        inputInfo.setAccountWay("STMCD-EM");
        inputInfo.setLongitude("112.981886");
        inputInfo.setHouseFundStatus("10");
        inputInfo.setPsbcDaId("bh2vst8w2084977801336229888");
        inputInfo.setPreCrdtLimit("0.0");
        inputInfo.setSelfflags("0");
        inputInfo.setIcbcStatus("10");
        inputInfo.setIdno("430105199102050024");
        inputInfo.setIdtype("ETHNC-01");
        inputInfo.setAccountDay("18");
        inputInfo.setOnlineType("2");
        inputInfo.setZksqrCertLimitTime("20341203");
        inputInfo.setIsAuditPass("0");
        inputInfo.setStatus("089-X");
        inputInfo.setIcbcDaId("bh2vst8w2084991847041110016");
        inputInfo.setTradeno("OCCCD-A");
        inputInfo.setLatitude("28.219226");
        inputInfo.setZksqrName("ZAO/SHUYANXIA/");
        inputInfo.setOnlineApplyStatus("931");
        inputInfo.setMainname("造数檐下");
        inputInfo.setTaxDaId("bh2vst8w2084979870151512064");
        inputInfo.setPsbcStatus("10");
        inputInfo.setNational("NATCD-CHN");
        inputInfo.setCardtype("0064");
        inputInfo.setDepartment("022201");
        inputInfo.setMaindate("19910205");
        inputInfo.setMainSequenceno("2602098864000001");
        inputInfo.setZkkpbmCode("CDFRM-A");
        inputInfo.setMobile("15231625168");
        inputInfo.setFaceEffect("N");
        inputInfo.setRisklevel("CRISK-2");
        inputInfo.setAlipayStatus("10");
        inputInfo.setSequenceno("2602098864000001");
        inputInfo.setWeChatStatus("51");
        inputInfo.setSqxyLimit("90000.00");
        inputInfo.setAlipayDaId("bh2vst8w2084983331177926656");
        inputInfo.setIdcardIsLongtimeIdent("0");
        inputInfo.setWereAlsoPStatus("0");
        inputInfo.setHouseFundDaId("bh2vst8w2084983968364007424");
        inputInfo.setSpreaderCode("3980");
        inputInfo.setWeChatDaId("bh2vst8w2084980598110720000");
        inputInfo.setMainnation("NATON-1");
        inputInfo.setAutoSendOrdersFlag("0");
        inputInfo.setTransWay("CDESP-POST");
        return inputInfo;
    }

    /**
     * 构建 bhdata（模拟外部系统返回的数据）
     */
    private static BhData buildBhData() {
        BhData bhdata = new BhData();
        bhdata.setTaxDaId("bh2vst8w2084979870151512064_new");
        bhdata.setTaxStatus(1);
        bhdata.setPsbcDaId("bh2vst8w2084977801336229888_new");
        bhdata.setPsbcStatus(1);
        bhdata.setHouseFundDaId("bh2vst8w2084983968364007424_new");
        bhdata.setHouseFundStatus(1);
        bhdata.setAlipayDaId("bh2vst8w2084983331177926656_new");
        bhdata.setAlipayStatus(1);
        bhdata.setWeChatDaId("bh2vst8w2084980598110720000_new");
        bhdata.setWeChatStatus(1);
        bhdata.setIcbcDaId("bh2vst8w2084991847041110016_new");
        bhdata.setIcbcStatus(1);
        return bhdata;
    }

    // ==================== 实体类定义 ====================

    @Data
    public static class InputInfo {
        private String engname;
        private String nationl;
        private String dateOfFilling;
        private String mainsex;
        private String wereAlsoIsPers;
        private String deviceId;
        private String opine;
        private String taxStatus;
        private String zaCode;
        private String remoteInterviewFlag;
        private String audittype;
        private String feetype;
        private String promotermobile;
        private String spreaderName;
        private String accountWay;
        private String longitude;
        private String houseFundStatus;
        private String psbcDaId;
        private String preCrdtLimit;
        private String selfflags;
        private String icbcStatus;
        private String idno;
        private String idtype;
        private String accountDay;
        private String onlineType;
        private String zksqrCertLimitTime;
        private String isAuditPass;
        private String status;
        private String icbcDaId;
        private String tradeno;
        private String latitude;
        private String zksqrName;
        private String onlineApplyStatus;
        private String mainname;
        private String taxDaId;
        private String psbcStatus;
        private String national;
        private String cardtype;
        private String department;
        private String maindate;
        private String mainSequenceno;
        private String zkkpbmCode;
        private String mobile;
        private String faceEffect;
        private String risklevel;
        private String alipayStatus;
        private String sequenceno;
        private String weChatStatus;
        private String sqxyLimit;
        private String alipayDaId;
        private String idcardIsLongtimeIdent;
        private String wereAlsoPStatus;
        private String houseFundDaId;
        private String spreaderCode;
        private String weChatDaId;
        private String mainnation;
        private String autoSendOrdersFlag;
        private String transWay;
    }

    @Data
    public static class BhData {
        private String taxDaId;
        private Integer taxStatus;
        private String psbcDaId;
        private Integer psbcStatus;
        private String houseFundDaId;
        private Integer houseFundStatus;
        private String alipayDaId;
        private Integer alipayStatus;
        private String weChatDaId;
        private Integer weChatStatus;
        private String icbcDaId;
        private Integer icbcStatus;
    }

    @Data
    public static class SMHL0060Req {
        private List<SMHL0060ReqList> list;
    }

    @Data
    public static class SMHL0060ReqList {
        private String sqjNo;
        private String inputScene;
        private String inputFlag;
        private String inputInfo;  // 必须是 String，不能改
    }

    @FunctionalInterface
    private interface InputInfoConverter {
        String convert(InputInfo info);
    }
}