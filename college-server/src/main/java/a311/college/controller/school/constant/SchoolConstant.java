package a311.college.controller.school.constant;

import a311.college.entity.major.Major;
import a311.college.entity.school.School;
import a311.college.enumeration.ProvinceEnum;
import a311.college.vo.major.HotMajorVO;

import java.util.ArrayList;
import java.util.List;

/**
 * School常量
 */
public class SchoolConstant {

    /**
     * 获取热门本科专业
     *
     * @return List<HotMajorVO>
     */
    public static List<HotMajorVO> getHotMajor() {
        List<HotMajorVO> hotMajorVOList = new ArrayList<>();
        hotMajorVOList.add(new HotMajorVO(21,"人工智能", "工学,计算机类", "清华大学", 140, 691));
        hotMajorVOList.add(new HotMajorVO(23,"电子科学与技术", "工学,电子信息类", "电子科技大学", 661, 662));
        hotMajorVOList.add(new HotMajorVO(274,"临床医学", "医学,临床医学类", "上海交通大学", 125, 688));
        hotMajorVOList.add(new HotMajorVO(524,"法学", "法学,法学类", "中国政法大学", 569, 665));
        hotMajorVOList.add(new HotMajorVO(121,"自动化", "工学,自动化类", "哈尔滨工业大学", 34, 664));
        hotMajorVOList.add(new HotMajorVO(5,"数据科学与大数据技术", "工学,计算机类", "北京大学", 31, 680));
        hotMajorVOList.add(new HotMajorVO(161,"能源与动力工程", "工学,能源动力类", "华中科技大学", 127, 658));
        hotMajorVOList.add(new HotMajorVO(4,"物联网工程", "工学,电子信息类", "西安电子科技大学", 57, 645));
        hotMajorVOList.add(new HotMajorVO(2,"软件工程", "工学,计算机类", "浙江大学", 114, 672));
        hotMajorVOList.add(new HotMajorVO(366,"汉语言文学", "文学,中国语言文学类", "北京师范大学", 52, 643));
        return hotMajorVOList;
    }

    /**
     * 获取热门专科专业
     *
     * @return List<HotMajorVO>
     */
    public static List<HotMajorVO> getHotProfessionalMajor() {
        List<HotMajorVO> hotMajorVOList = new ArrayList<>();
        hotMajorVOList.add(new HotMajorVO(779, "护理", "医药卫生大类,护理类", "南昌职业大学", 909, 291));
        hotMajorVOList.add(new HotMajorVO(753,"工业设计", "装备制造大类,机械设计类", "西安汽车职业大学", 2257, 383));
        hotMajorVOList.add(new HotMajorVO(634,"学前教育", "教育与体育大类,教育类", "南昌职业大学", 909, 290));
        hotMajorVOList.add(new HotMajorVO(869,"动物医学", "农林牧渔大类,畜牧业类", "深圳职业技术大学", 646, 504));
        hotMajorVOList.add(new HotMajorVO(728,"数字媒体技术", "文化艺术大类,艺术设计类", "上海工艺美术职业学院", 671, 240));
        hotMajorVOList.add(new HotMajorVO(947,"智能采矿技术", "资源环境与安全大类,地质类", "湖南工程职业技术学院", 2003, 423));
        hotMajorVOList.add(new HotMajorVO(782,"医学检验技术", "医药卫生大类,医学技术类", "贵阳康养职业大学", 2274, 415));
        hotMajorVOList.add(new HotMajorVO(863,"航海技术", "交通运输大类,水上运输类", "贵阳康养职业大学", 2274, 283));
        hotMajorVOList.add(new HotMajorVO(781,"口腔医学技术", "资源环境与安全大类,地质类", "湖南工程职业技术学院", 2003, 502));
        hotMajorVOList.add(new HotMajorVO(729,"软件工程技术", "电子与信息大类,计算机类", "南京工业职业技术大学", 1065, 477));
        return hotMajorVOList;
    }

    /**
     * 获取默认学校当搜索提示
     *
     * @return List<SchoolVO>
     */
    public static List<School> getSchool() {
        List<School> schoolVOList = new ArrayList<>();
        schoolVOList.add(new School(31, null, "北京大学", ProvinceEnum.getProvince("北京"), null, "985,211,双一流", null, null, null, null));
        schoolVOList.add(new School(140, null, "清华大学", ProvinceEnum.getProvince("北京"), null, "985,211,双一流", null, null, null, null));
        schoolVOList.add(new School(114, null, "浙江大学", ProvinceEnum.getProvince("浙江"), null, "985,211,双一流", null, null, null, null));
        schoolVOList.add(new School(132, null, "复旦大学", ProvinceEnum.getProvince("上海"), null, "985,211,双一流", null, null, null, null));
        schoolVOList.add(new School(42, null, "武汉大学", ProvinceEnum.getProvince("武汉"), null, "985,211,双一流", null, null, null, null));
        return schoolVOList;
    }

    /**
     * 获取默认专业当搜索提示
     *
     * @return List<Major>
     */
    public static List<Major> getMajor() {
        List<Major> majorList = new ArrayList<>();
        majorList.add(new Major("工学", "计算机类", 1, "计算机科学与技术", null, null, null, "66:34", 14200));
        majorList.add(new Major("工学", "计算机类", 21, "人工智能", null, null, null, "70:30", 17200));
        majorList.add(new Major("工学", "电子信息类", 19, "电子信息工程", null, null, null, "73:27", 12900));
        majorList.add(new Major("文学", "中国语言文学类", 366, "汉语言文学", null, null, null, "16:84", 10900));
        majorList.add(new Major("医学", "临床医学类", 274, "临床医学", null, null, null, "44:56", 13000));
        return majorList;
    }

    /**
     * 获取热门大学排行榜
     *
     * @return List<School>
     */
    public static List<School> getHotRank() {
        List<School> schoolVOList = new ArrayList<>();
        schoolVOList.add(new School(102, "https://static-data.gaokao.cn/upload/logo/102.jpg", "厦门大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(99, "https://static-data.gaokao.cn/upload/logo/99.jpg", "四川大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(42, "https://static-data.gaokao.cn/upload/logo/42.jpg", "武汉大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(104, "https://static-data.gaokao.cn/upload/logo/104.jpg", "中山大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(140, "https://static-data.gaokao.cn/upload/logo/140.jpg", "清华大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(123, "https://static-data.gaokao.cn/upload/logo/123.jpg", "中南大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(111, "https://static-data.gaokao.cn/upload/logo/111.jpg", "南京大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(934, "https://static-data.gaokao.cn/upload/logo/934.jpg", "西南大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(31, "https://static-data.gaokao.cn/upload/logo/31.jpg", "北京大学", null, null, null, null, null, null, null));
        schoolVOList.add(new School(661, "https://static-data.gaokao.cn/upload/logo/661.jpg", "电子科技大学", null, null, null, null, null, null, null));
        return schoolVOList;
    }


    public static List<String> get1038() {
        List<String> images = new ArrayList<>();
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E6%96%87%E7%90%861.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E6%96%87%E7%90%862.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E6%96%87%E7%90%863.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E6%96%87%E7%90%864.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E6%96%87%E7%90%865.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E6%96%87%E7%90%866.jpg");
        return images;
    }

    public static List<String> get119() {
        List<String> images = new ArrayList<>();
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E5%A4%A7%E5%AD%A61.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E5%A4%A7%E5%AD%A62.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E5%A4%A7%E5%AD%A63.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E5%A4%A7%E5%AD%A64.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E5%A4%A7%E5%AD%A65.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E5%A4%A7%E5%AD%A66.jpg");
        return images;
    }

    public static List<String> get934() {
        List<String> images = new ArrayList<>();
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E8%A5%BF%E5%8D%97%E5%A4%A7%E5%AD%A61.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E8%A5%BF%E5%8D%97%E5%A4%A7%E5%AD%A62.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E8%A5%BF%E5%8D%97%E5%A4%A7%E5%AD%A63.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E8%A5%BF%E5%8D%97%E5%A4%A7%E5%AD%A64.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E8%A5%BF%E5%8D%97%E5%A4%A7%E5%AD%A65.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E8%A5%BF%E5%8D%97%E5%A4%A7%E5%AD%A66.jpg");
        return images;
    }

    public static List<String> get184() {
        List<String> images = new ArrayList<>();
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E9%82%AE%E7%94%B51.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E9%82%AE%E7%94%B52.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E9%82%AE%E7%94%B53.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E9%82%AE%E7%94%B54.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E9%82%AE%E7%94%B55.jpg");
        images.add("https://wzb-study.oss-cn-chengdu.aliyuncs.com/%E9%87%8D%E5%BA%86%E9%82%AE%E7%94%B56.jpg");
        return images;
    }


}
