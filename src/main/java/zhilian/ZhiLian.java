package zhilian;

import boss.BossConfig;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JSONUtils;
import utils.Job;
import utils.JobUtils;
import utils.SeleniumUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static utils.Bot.sendMessageByTime;
import static utils.Constant.*;
import static utils.JobUtils.formatDuration;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
public class ZhiLian {
    static {
        // 在类加载时就设置日志文件名，确保Logger初始化时能获取到正确的属性
        System.setProperty("log.name", "zhilian");
    }
    
    private static final Logger log = LoggerFactory.getLogger(ZhiLian.class);
    static String loginUrl = "https://passport.zhaopin.com/login";
    static String homeUrl = "https://sou.zhaopin.com/?";
    static boolean isLimit = false;
    static int maxPage = 500;
    static ZhilianConfig config = ZhilianConfig.init();
    static List<Job> resultList = new ArrayList<>();
    static Date startDate;

    static Set<String> blackCompanies;
    static Set<String> blackRecruiters;
    static Set<String> blackJobs;
    static String dataPath = "src/main/java/boss/data.json";

    static {
        try {
            // 检查dataPath文件是否存在，不存在则创建
            File dataFile = new File(dataPath);
            if (!dataFile.exists()) {
                // 确保父目录存在
                if (!dataFile.getParentFile().exists()) {
                    dataFile.getParentFile().mkdirs();
                }
                // 创建文件并写入初始JSON结构
                Map<String, Set<String>> initialData = new HashMap<>();
                initialData.put("blackCompanies", new HashSet<>());
                initialData.put("blackRecruiters", new HashSet<>());
                initialData.put("blackJobs", new HashSet<>());
                initialData.put("cityArea", new HashSet<>());
                String initialJson = JSONUtils.customJsonFormat(initialData);
                Files.write(Paths.get(dataPath), initialJson.getBytes());
                log.info("创建数据文件: {}", dataPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        SeleniumUtil.initDriver();
        startDate = new Date();
        login();
        config.getKeywords().forEach(keyword -> {
            if (isLimit) {
                return;
            }
            CHROME_DRIVER.get(getSearchUrl(keyword, 1));
            submitJobs(keyword);

        });
        log.info(resultList.isEmpty() ? "未投递新的岗位..." : "新投递公司如下:\n{}", resultList.stream().map(Object::toString).collect(Collectors.joining("\n")));
        printResult();
    }

    private static void printResult() {
        String message = String.format("\n智联招聘投递完成，共投递%d个岗位，用时%s", resultList.size(), formatDuration(startDate, new Date()));
        log.info(message);
        sendMessageByTime(message);
        resultList.clear();
        CHROME_DRIVER.close();
        CHROME_DRIVER.quit();
        
        // 确保所有日志都被刷新到文件
        try {
            Thread.sleep(1000); // 等待1秒确保日志写入完成
            // 强制刷新日志 - 使用正确的方法
            ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            loggerContext.stop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String getSearchUrl(String keyword, int page) {
        return homeUrl +
                JobUtils.appendParam("jl", config.getCityCode()) +
                JobUtils.appendParam("kw", keyword) +
                JobUtils.appendParam("sl", config.getSalary()) +
                "&p=" + page;
    }

    private static void submitJobs(String keyword) {
        if (isLimit) {
            return;
        }
        WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'joblist-box__item')]")));
//        setMaxPages();
        for (int i = 1; i <= maxPage; i++) {
            if (i != 1) {
                CHROME_DRIVER.get(getSearchUrl(keyword, i));
            }
            log.info("开始投递【{}】关键词，第【{}】页...", keyword, i);
            // 等待岗位出现
            try {
                WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='positionlist']")));
            } catch (Exception ignore) {
                CHROME_DRIVER.navigate().refresh();
                SeleniumUtil.sleep(1);
            }
            // 全选
//            try {
//                WebElement allSelect = WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//i[@class='betch__checkall__checkbox']")));
//                allSelect.click();
//            } catch (Exception e) {
//                log.info("没有全选按钮，程序退出...");
//                continue;
//            }
            // 投递
//            WebElement submit = WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@class='betch__button']")));
//            submit.click();
//            if (checkIsLimit()) {
//                break;
//            }
            WebElement jobCardList = WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='positionlist__list']")));
            List<WebElement> elements = jobCardList.findElements(By.className("joblist-box__item"));
            // 保存主窗口句柄
            String mainWindow = CHROME_DRIVER.getWindowHandle();
            // 获取到当前页所有岗位
            for (WebElement element : elements) {
                try {
                    // 解析岗位信息
                    Job job = parseJobInfo(element);
                    SeleniumUtil.sleep(2);
                    // 点击投递按钮
                    WebElement applyBtn = element.findElement(By.xpath(".//button[@class='collect-and-apply__btn']"));
                    applyBtn.click();
                    SeleniumUtil.sleep(1);
                    
                    // 处理新打开的标签页
                    Set<String> allWindows = CHROME_DRIVER.getWindowHandles();
                    if (allWindows.size() > 1) {
                        for (String window : allWindows) {
                            if (!window.equals(mainWindow)) {
                                CHROME_DRIVER.switchTo().window(window);
                                SeleniumUtil.sleep(1);
                                CHROME_DRIVER.close();
                                SeleniumUtil.sleep(1);
                                break;
                            }
                        }
                        CHROME_DRIVER.switchTo().window(mainWindow);
                    }
                    
                    // 记录投递结果
                    resultList.add(job);
                    log.info("投递【{}】公司【{}】岗位，薪资【{}】，地区【{}】", 
                            job.getCompanyName(), job.getJobName(), job.getSalary(), job.getJobInfo());
                    
                    SeleniumUtil.sleep(1);
                } catch (Exception e) {
                    log.error("投递岗位失败: {}", e.getMessage());
                }
            }
            // 当前页处理完了
            log.info("第【{}】页岗位投递完成！", i);
            // 切换到新的标签页

            //关闭弹框
//            try {
//                WebElement result = CHROME_DRIVER.findElement(By.xpath("//div[@class='deliver-dialog']"));
//                if (result.getText().contains("申请成功")) {
//                    log.info("岗位申请成功！");
//                }
//            } catch (Exception e) {
//                log.error("关闭投递弹框失败...");
//            }
//            try {
//                WebElement close = CHROME_DRIVER.findElement(By.xpath("//img[@title='close-icon']"));
//                close.click();
//            } catch (Exception e) {
//                if (checkIsLimit()) {
//                    break;
//                }
//            }
//            try {
//                // 投递相似职位
//                WebElement checkButton = CHROME_DRIVER.findElement(By.xpath("//div[contains(@class, 'applied-select-all')]//input"));
//                if (!checkButton.isSelected()) {
//                    checkButton.click();
//                }
//                List<WebElement> jobs = CHROME_DRIVER.findElements(By.xpath("//div[@class='recommend-job']"));
//                WebElement post = CHROME_DRIVER.findElement(By.xpath("//div[contains(@class, 'applied-select-all')]//button"));
//                post.click();
//                printRecommendJobs(jobs);
//                log.info("相似职位投递成功！");
//            } catch (NoSuchElementException e) {
//                log.error("没有匹配到相似职位...");
//            } catch (Exception e) {
//                log.error("相似职位投递异常！！！");
//            }
            // 投完了关闭当前窗口并切换至第一个窗口
        }
    }

    private static boolean checkIsLimit() {
        try {
            SeleniumUtil.sleepByMilliSeconds(500);
            WebElement result = CHROME_DRIVER.findElement(By.xpath("//div[@class='a-job-apply-workflow']"));
            if (result.getText().contains("达到上限")) {
                log.info("今日投递已达上限！");
                isLimit = true;
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static void setMaxPages() {
        try {
            // 到底部
            ACTIONS.keyDown(Keys.CONTROL).sendKeys(Keys.END).keyUp(Keys.CONTROL).perform();
            WebElement inputElement = CHROME_DRIVER.findElement(By.className("soupager__pagebox__goinp"));
            inputElement.clear();
            inputElement.sendKeys("99999");
            //使用 JavaScript 获取输入元素的当前值
            JavascriptExecutor js = CHROME_DRIVER;
            String modifiedValue = (String) js.executeScript("return arguments[0].value;", inputElement);
            maxPage = Integer.parseInt(modifiedValue);
            log.info("设置最大页数：{}", maxPage);
            WebElement home = CHROME_DRIVER.findElement(By.xpath("//li[@class='listsort__item']"));
            ACTIONS.moveToElement(home).perform();
        } catch (Exception ignore) {
            StackTraceElement element = Thread.currentThread().getStackTrace()[1];
            log.info("setMaxPages@设置最大页数异常！({}:{})", element.getFileName(), element.getLineNumber());
            log.info("设置默认最大页数50，如有需要请自行调整...");
            maxPage = 50;
        }
    }

    private static Job parseJobInfo(WebElement element) {
        Job job = new Job();
        
        // 岗位名称
        try {
            WebElement jobNameElement = element.findElement(By.xpath(".//a[@class='jobinfo__name']"));
            job.setJobName(jobNameElement.getText().trim());
        } catch (Exception e) {
            log.warn("解析岗位名称失败");
            job.setJobName("未知岗位");
        }
        
        // 薪资
        try {
            WebElement salaryElement = element.findElement(By.xpath(".//p[@class='jobinfo__salary']"));
            job.setSalary(salaryElement.getText().trim());
        } catch (Exception e) {
            log.warn("解析薪资失败");
            job.setSalary("面议");
        }
        
        // 工作区域、经验和学历
        try {
            List<WebElement> otherInfos = element.findElements(By.xpath(".//div[@class='jobinfo__other-info-item']"));
            String location = "";
            String experience = "";
            String education = "";
            
            if (!otherInfos.isEmpty()) {
                // 第一个包含地区信息
                try {
                    location = otherInfos.get(0).findElement(By.tagName("span")).getText().trim();
                } catch (Exception e) {
                    location = otherInfos.get(0).getText().trim();
                }
            }
            if (otherInfos.size() > 1) {
                experience = otherInfos.get(1).getText().trim();
            }
            if (otherInfos.size() > 2) {
                education = otherInfos.get(2).getText().trim();
            }
            
            job.setJobInfo(location + (experience.isEmpty() ? "" : "·" + experience) + (education.isEmpty() ? "" : "·" + education));
        } catch (Exception e) {
            log.warn("解析工作信息失败: {}", e.getMessage());
            job.setJobInfo("未知");
        }
        
        // 公司名称
        try {
            WebElement companyNameElement = element.findElement(By.xpath(".//a[@class='companyinfo__name']"));
            job.setCompanyName(companyNameElement.getText().trim());
        } catch (Exception e) {
            log.warn("解析公司名称失败");
            job.setCompanyName("未知公司");
        }
        
        // 公司标签
        try {
            List<WebElement> companyTags = element.findElements(By.xpath(".//div[@class='companyinfo__tag']//div[@class='joblist-box__item-tag']"));
            if (!companyTags.isEmpty()) {
                String companyTag = companyTags.stream()
                        .map(WebElement::getText)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("·"));
                job.setCompanyTag(companyTag);
            } else {
                job.setCompanyTag("");
            }
        } catch (Exception e) {
            log.warn("解析公司标签失败");
            job.setCompanyTag("");
        }
        
        return job;
    }

    private static void printRecommendJobs(List<WebElement> jobs) {
        jobs.forEach(j -> {
            String jobName = j.findElement(By.xpath(".//*[contains(@class, 'recommend-job__position')]")).getText();
            String salary = j.findElement(By.xpath(".//span[@class='recommend-job__demand__salary']")).getText();
            String years = j.findElement(By.xpath(".//span[@class='recommend-job__demand__experience']")).getText().replaceAll("\n", " ");
            String education = j.findElement(By.xpath(".//span[@class='recommend-job__demand__educational']")).getText().replaceAll("\n", " ");
            String companyName = j.findElement(By.xpath(".//*[contains(@class, 'recommend-job__cname')]")).getText();
            String companyTag = j.findElement(By.xpath(".//*[contains(@class, 'recommend-job__demand__cinfo')]")).getText().replaceAll("\n", " ");
            Job job = new Job();
            job.setJobName(jobName);
            job.setSalary(salary);
            job.setCompanyTag(companyTag);
            job.setCompanyName(companyName);
            job.setJobInfo(years + "·" + education);
            log.info("投递【{}】公司【{}】岗位，薪资【{}】，要求【{}·{}】，规模【{}】", companyName, jobName, salary, years, education, companyTag);
            resultList.add(job);
        });
    }

    private static void login() {
        CHROME_DRIVER.get(loginUrl);
        if (SeleniumUtil.isCookieValid("./src/main/java/zhilian/cookie.json")) {
            SeleniumUtil.loadCookie("./src/main/java/zhilian/cookie.json");
            CHROME_DRIVER.navigate().refresh();
            SeleniumUtil.sleep(1);
        }
        if (isLoginRequired()) {
            scanLogin();
        }
    }

    private static void scanLogin() {
        try {
            WebElement button = CHROME_DRIVER.findElement(By.xpath("//div[@class='zppp-panel-normal-bar__img']"));
            button.click();
            log.info("等待扫码登录中...");
            WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='zp-main__personal']")));
            log.info("扫码登录成功！");
            SeleniumUtil.saveCookie("./src/main/java/zhilian/cookie.json");
        } catch (Exception e) {
            log.error("扫码登录异常！");
            System.exit(-1);
        }
    }

    private static boolean isLoginRequired() {
        return !CHROME_DRIVER.getCurrentUrl().contains("i.zhaopin.com");
    }

}
