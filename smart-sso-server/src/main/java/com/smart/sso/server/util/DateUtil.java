package com.smart.sso.server.util;


import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * 日期操作工具类，主要实现了日期的常用操作。
 * <p>
 * 在工具类中经常使用到工具类的格式化描述，这个主要是一个日期的操作类，所以日志格式主要使用 SimpleDateFormat的定义格式.
 * <p>
 * 格式的意义如下： 日期和时间模式 <br>
 * 日期和时间格式由日期和时间模式字符串指定。在日期和时间模式字符串中，未加引号的字母 'A' 到 'Z' 和 'a' 到 'z'
 * 被解释为模式字母，用来表示日期或时间字符串元素。文本可以使用单引号 (') 引起来，以免进行解释。"''"
 * 表示单引号。所有其他字符均不解释；只是在格式化时将它们简单复制到输出字符串，或者在分析时与输入字符串进行匹配。
 * <p>
 * <p>
 * 模式字母通常是重复的，其数量确定其精确表示：
 */
@Slf4j
public final class DateUtil implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -3098985139095632110L;

    private DateUtil() {
    }

    /**
     * 格式化日期显示格式
     *
     * @param sdate  原始日期格式 s - 表示 "yyyy-mm-dd" 形式的日期的 String 对象
     * @param format 格式化后日期格式
     * @return 格式化后的日期显示
     */
    public static String dateFormat(String sdate, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        java.sql.Date date = java.sql.Date.valueOf(sdate);
        String dateString = formatter.format(date);

        return dateString;
    }

    /**
     * 求两个日期相差天数
     *
     * @param sd 起始日期，格式yyyy-MM-dd
     * @param ed 终止日期，格式yyyy-MM-dd
     * @return 两个日期相差天数
     */
    public static long getIntervalDays(String sd, String ed) {
        return ((java.sql.Date.valueOf(ed)).getTime() - (java.sql.Date
                .valueOf(sd)).getTime())
                / (3600 * 24 * 1000);
    }

    /**
     * 起始年月yyyy-MM与终止月yyyy-MM之间跨度的月数。
     *
     * @param beginMonth 格式为yyyy-MM
     * @param endMonth   格式为yyyy-MM
     * @return 整数。
     */
    public static int getInterval(String beginMonth, String endMonth) {
        int intBeginYear = Integer.parseInt(beginMonth.substring(0, 4));
        int intBeginMonth = Integer.parseInt(beginMonth.substring(beginMonth
                .indexOf("-") + 1));
        int intEndYear = Integer.parseInt(endMonth.substring(0, 4));
        int intEndMonth = Integer.parseInt(endMonth.substring(endMonth
                .indexOf("-") + 1));

        return ((intEndYear - intBeginYear) * 12)
                + (intEndMonth - intBeginMonth) + 1;
    }

    /**
     * 根据给定的分析位置开始分析日期/时间字符串。例如，时间文本 "07/10/96 4:5 PM, PDT" 会分析成等同于
     * Date(837039928046) 的 Date。
     *
     * @param sDate
     * @param dateFormat
     * @return
     */
    public static Date getDate(String sDate, String dateFormat) {
        SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);
        ParsePosition pos = new ParsePosition(0);

        return fmt.parse(sDate, pos);
    }

    /**
     * 取得当前日期的年份，以yyyy格式返回.
     *
     * @return 当年 yyyy
     */
    public static String getCurrentYear() {
        return getFormatCurrentTime("yyyy");
    }

    /**
     * 自动返回上一年。例如当前年份是2007年，那么就自动返回2006
     *
     * @return 返回结果的格式为 yyyy
     */
    public static String getBeforeYear() {
        String currentYear = getFormatCurrentTime("yyyy");
        int beforeYear = Integer.parseInt(currentYear) - 1;
        return "" + beforeYear;
    }

    /**
     * 取得当前日期的月份，以MM格式返回.
     *
     * @return 当前月份 MM
     */
    public static String getCurrentMonth() {
        return getFormatCurrentTime("MM");
    }

    /**
     * 只返回月份数字
     *
     * @return
     */
    public static String getCurrentMonthWithoutPrefix() {
        return String.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1);
    }

    /**
     * 取得当前日期的天数，以格式"dd"返回.
     *
     * @return 当前月中的某天dd
     */
    public static String getCurrentDay() {
        return getFormatCurrentTime("dd");
    }

    /**
     * 返回当前时间字符串。
     * <p>
     * 格式：yyyy-MM-dd
     *
     * @return String 指定格式的日期字符串.
     */
    public static String getCurrentDate() {
        return getFormatDateTime(new Date(), "yyyy-MM-dd");
    }

    /**
     * 返回当前指定的时间戳。格式为yyyy-MM-dd HH:mm:ss
     *
     * @return 格式为yyyy-MM-dd HH:mm:ss，总共19位。
     */
    public static String getCurrentDateTime() {
        return getFormatDateTime(new Date(), "yyyy-MM-dd HH:mm:ss");
    }

    public static String getCurrentTimeWithMs() {
        return getFormatDateTime(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
    }

    /**
     * 返回给定时间字符串。
     * <p>
     * 格式：yyyy-MM-dd
     *
     * @param date 日期
     * @return String 指定格式的日期字符串.
     */
    public static String getFormatDate(Date date) {
        return getFormatDateTime(date, "yyyy-MM-dd");
    }

    /**
     * 根据制定的格式，返回日期字符串。例如2007-05-05
     *
     * @param format "yyyy-MM-dd" 或者 "yyyy/MM/dd",当然也可以是别的形式。
     * @return 指定格式的日期字符串。
     */
    public static String getFormatDate(String format) {
        return getFormatDateTime(new Date(), format);
    }

    /**
     * 返回当前时间字符串。
     * <p>
     * 格式：yyyy-MM-dd HH:mm:ss
     *
     * @return String 指定格式的日期字符串.
     */
    public static String getCurrentTime() {
        return getFormatDateTime(new Date(), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 返回给定时间字符串。
     * <p>
     * 格式：yyyy-MM-dd HH:mm:ss
     *
     * @param date 日期
     * @return String 指定格式的日期字符串.
     */
    public static String getFormatTime(Date date) {
        return getFormatDateTime(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 根据给定的格式，返回时间字符串。
     * <p>
     * 格式参照类描绘中说明.和方法getFormatDate是一样的。
     *
     * @param format 日期格式字符串
     * @return String 指定格式的日期字符串.
     */
    public static String getFormatCurrentTime(String format) {
        return getFormatDateTime(new Date(), format);
    }

    /**
     * 根据给定的格式与时间(Date类型的)，返回时间字符串。最为通用。<br>
     *
     * @param date   指定的日期
     * @param format 日期格式字符串
     * @return String 指定格式的日期字符串.
     */
    public static String getFormatDateTime(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    /**
     * 取得指定年月日的日期对象.
     *
     * @param year  年
     * @param month 月注意是从1到12
     * @param day   日
     * @return 一个java.util.Date()类型的对象
     */
    public static Date getDateObj(int year, int month, int day) {
        Calendar c = new GregorianCalendar();
        c.set(year, month - 1, day);
        return c.getTime();
    }


    /**
     * 取得给定日期减去一定月数的日期对象.
     *
     * @param date   给定的日期对象
     * @param amount 需要添加的月数，如果是向前的天数，使用负数就可以.
     * @return Date 加上一定月数以后的Date对象.
     */
    public static String getMonthString(String date, int amount) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            Date sDate = sdf.parse(date);
            Calendar cal = new GregorianCalendar();
            cal.setTime(sDate);
            cal.add(GregorianCalendar.MONTH, amount);
            return sdf.format(cal.getTime());
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 获取指定日期的下一天。
     *
     * @param date yyyy/MM/dd
     * @return yyyy/MM/dd
     */
    public static String getDateTomorrow(String date) {

        Date tempDate = null;
        if (date.indexOf("/") > 0)
            tempDate = getDateObj(date, "[/]");
        if (date.indexOf("-") > 0)
            tempDate = getDateObj(date, "[-]");
        tempDate = getDateAdd(tempDate, 1);
        return getFormatDateTime(tempDate, "yyyy/MM/dd");
    }

    /**
     * 获取与指定日期相差指定天数的日期。
     *
     * @param date   yyyy/MM/dd
     * @param offset 正整数
     * @return yyyy/MM/dd
     */
    public static String getDateOffset(String date, int offset) {

        //Date tempDate = getDateObj(date, "[/]");
        Date tempDate = null;
        if (date.indexOf("/") > 0)
            tempDate = getDateObj(date, "[/]");
        if (date.indexOf("-") > 0)
            tempDate = getDateObj(date, "[-]");
        tempDate = getDateAdd(tempDate, offset);
        return getFormatDateTime(tempDate, "yyyy/MM/dd");
    }

    /**
     * 取得指定分隔符分割的年月日的日期对象.
     *
     * @param argsDate 格式为"yyyy-MM-dd"
     * @param split    时间格式的间隔符，例如“-”，“/”，要和时间一致起来。
     * @return 一个java.util.Date()类型的对象
     */
    public static Date getDateObj(String argsDate, String split) {
        String[] temp = argsDate.split(split);
        int year = new Integer(temp[0]).intValue();
        int month = new Integer(temp[1]).intValue();
        int day = new Integer(temp[2]).intValue();
        return getDateObj(year, month, day);
    }

    /**
     * 取得给定字符串描述的日期对象，描述模式采用pattern指定的格式.
     *
     * @param dateStr 日期描述 从给定字符串的开始分析文本，以生成一个日期。该方法不使用给定字符串的整个文本。 有关日期分析的更多信息，请参阅
     *                parse(String, ParsePosition) 方法。一个 String，应从其开始处进行分析
     * @param pattern 日期模式
     * @return 给定字符串描述的日期对象。
     */
    public static Date getDateFromString(String dateStr, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        Date resDate = null;
        try {
            resDate = sdf.parse(dateStr);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return resDate;
    }

    /**
     * 取得当前Date对象.
     *
     * @return Date 当前Date对象.
     */
    public static Date getDateObj() {
        Calendar c = new GregorianCalendar();
        return c.getTime();
    }

    /**
     * @return 当前月份有多少天；
     */
    public static int getDaysOfCurMonth() {
        int curyear = new Integer(getCurrentYear()).intValue(); // 当前年份
        int curMonth = new Integer(getCurrentMonth()).intValue();// 当前月份
        int mArray[] = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30,
                31};
        // 判断闰年的情况 ，2月份有29天；
        if ((curyear % 400 == 0)
                || ((curyear % 100 != 0) && (curyear % 4 == 0))) {
            mArray[1] = 29;
        }
        return mArray[curMonth - 1];
        // 如果要返回下个月的天数，注意处理月份12的情况，防止数组越界；
        // 如果要返回上个月的天数，注意处理月份1的情况，防止数组越界；
    }


    /**
     * 返回指定为年度为year月度month的月份内，第weekOfMonth个星期的第dayOfWeek天是当月的几号。<br>
     * 00 00 00 01 02 03 04 <br>
     * 05 06 07 08 09 10 11<br>
     * 12 13 14 15 16 17 18<br>
     * 19 20 21 22 23 24 25<br>
     * 26 27 28 29 30 31 <br>
     * 2006年的第一个周的1到7天为：05 06 07 01 02 03 04 <br>
     * 2006年的第二个周的1到7天为：12 13 14 08 09 10 11 <br>
     * 2006年的第三个周的1到7天为：19 20 21 15 16 17 18 <br>
     * 2006年的第四个周的1到7天为：26 27 28 22 23 24 25 <br>
     * 2006年的第五个周的1到7天为：02 03 04 29 30 31 01 。本月没有就自动转到下个月了。
     *
     * @param year        形式为yyyy <br>
     * @param month       形式为MM,参数值在[1-12]。<br>
     * @param weekOfMonth 在[1-6],因为一个月最多有6个周。<br>
     * @param dayOfWeek   数字在1到7之间，包括1和7。1表示星期天，7表示星期六<br>
     *                    -6为星期日-1为星期五，0为星期六 <br>
     * @return <type>int</type>
     */
    public static int getDayofWeekInMonth(String year, String month,
                                          String weekOfMonth, String dayOfWeek) {
        Calendar cal = new GregorianCalendar();
        // 在具有默认语言环境的默认时区内使用当前时间构造一个默认的 GregorianCalendar。
        int y = new Integer(year).intValue();
        int m = new Integer(month).intValue();
        cal.clear();// 不保留以前的设置
        cal.set(y, m - 1, 1);// 将日期设置为本月的第一天。
        cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, new Integer(weekOfMonth)
                .intValue());
        cal.set(Calendar.DAY_OF_WEEK, new Integer(dayOfWeek).intValue());
        // System.out.print(cal.get(Calendar.MONTH)+" ");
        // System.out.print("当"+cal.get(Calendar.WEEK_OF_MONTH)+"\t");
        // WEEK_OF_MONTH表示当天在本月的第几个周。不管1号是星期几，都表示在本月的第一个周
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 根据指定的年月日小时分秒，返回一个java.Util.Date对象。
     *
     * @param year      年
     * @param month     月 0-11
     * @param date      日
     * @param hourOfDay 小时 0-23
     * @param minute    分 0-59
     * @param second    秒 0-59
     * @return 一个Date对象。
     */
    public static Date getDate(int year, int month, int date, int hourOfDay,
                               int minute, int second) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month, date, hourOfDay, minute, second);
        return cal.getTime();
    }

    /**
     * 根据指定的年、月、日返回当前是星期几。1表示星期天、2表示星期一、7表示星期六。
     *
     * @param year
     * @param month month是从1开始的12结束
     * @param day
     * @return 返回一个代表当期日期是星期几的数字。1表示星期天、2表示星期一、7表示星期六。
     */
    public static int getDayOfWeek(String year, String month, String day) {
        Calendar cal = new GregorianCalendar(new Integer(year).intValue(),
                new Integer(month).intValue() - 1, new Integer(day).intValue());
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * 根据指定的年、月、日返回当前是星期几。1表示星期天、2表示星期一、7表示星期六。
     *
     * @param date "yyyy/MM/dd",或者"yyyy-MM-dd"
     * @return 返回一个代表当期日期是星期几的数字。1表示星期天、2表示星期一、7表示星期六。
     */
    public static int getDayOfWeek(String date) {
        String[] temp = null;
        if (date.indexOf("/") > 0) {
            temp = date.split("/");
        }
        if (date.indexOf("-") > 0) {
            temp = date.split("-");
        }
        return getDayOfWeek(temp[0], temp[1], temp[2]);
    }


    /**
     * 根据指定的年、月、日返回当前是星期几。1表示星期天、2表示星期一、7表示星期六。
     *
     * @param date
     * @return 返回一个代表当期日期是星期几的数字。1表示星期天、2表示星期一、7表示星期六。
     */
    public static int getDayOfWeek(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * 返回制定日期所在的周是一年中的第几个周。<br>
     * created by wangmj at 20060324.<br>
     *
     * @param year
     * @param month 范围1-12<br>
     * @param day
     * @return int
     */
    public static int getWeekOfYear(String year, String month, String day) {
        Calendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(new Integer(year).intValue(),
                new Integer(month).intValue() - 1, new Integer(day).intValue());
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * 取得给定日期加上一定天数后的日期对象.
     *
     * @param date   给定的日期对象
     * @param amount 需要添加的天数，如果是向前的天数，使用负数就可以.
     * @return Date 加上一定天数以后的Date对象.
     */
    public static Date getDateAdd(Date date, int amount) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(GregorianCalendar.DATE, amount);
        return cal.getTime();
    }

    /**
     * 取得给定日期减去一定月数的日期对象.
     *
     * @param date   给定的日期对象
     * @param amount 需要添加的月数，如果是向前的天数，使用负数就可以.
     * @return Date 加上一定月数以后的Date对象.
     */
    public static Date getMonthAdd(Date date, int amount) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(GregorianCalendar.MONTH, amount);
        return cal.getTime();
    }

    /**
     * 取得给定日期加上一定天数后的日期对象.
     *
     * @param date   给定的日期对象
     * @param amount 需要添加的天数，如果是向前的天数，使用负数就可以.
     * @param format 输出格式.
     * @return Date 加上一定天数以后的Date对象.
     */
    public static String getFormatDateAdd(Date date, int amount, String format) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(GregorianCalendar.DATE, amount);
        return getFormatDateTime(cal.getTime(), format);
    }

    /**
     * 获得当前日期固定间隔天数的日期，如前60天dateAdd(-60)
     *
     * @param amount 距今天的间隔日期长度，向前为负，向后为正
     * @param format 输出日期的格式.
     * @return java.lang.String 按照格式输出的间隔的日期字符串.
     */
    public static String getFormatCurrentAdd(int amount, String format) {

        Date d = getDateAdd(new Date(), amount);

        return getFormatDateTime(d, format);
    }

    /**
     * 取得给定格式的昨天的日期输出
     *
     * @param format 日期输出的格式
     * @return String 给定格式的日期字符串.
     */
    public static String getFormatYestoday(String format) {
        return getFormatCurrentAdd(-1, format);
    }

    /**
     * 返回指定日期的前一天。<br>
     *
     * @param sourceDate
     * @param format     yyyy MM dd hh mm ss
     * @return 返回日期字符串，形式和formcat一致。
     */
    public static String getYestoday(String sourceDate, String format) {
        return getFormatDateAdd(getDateFromString(sourceDate, format), -1,
                format);
    }

    /**
     * 返回明天的日期，<br>
     *
     * @param format
     * @return 返回日期字符串，形式和formcat一致。
     */
    public static String getFormatTomorrow(String format) {
        return getFormatCurrentAdd(1, format);
    }

    /**
     * 返回指定日期的后一天。<br>
     *
     * @param sourceDate
     * @param format
     * @return 返回日期字符串，形式和formcat一致。
     */
    public static String getFormatDateTommorrow(String sourceDate, String format) {
        return getFormatDateAdd(getDateFromString(sourceDate, format), 1,
                format);
    }

    /**
     * 根据主机的默认 TimeZone，来获得指定形式的时间字符串。
     *
     * @param dateFormat
     * @return 返回日期字符串，形式和formcat一致。
     */
    public static String getCurrentDateString(String dateFormat) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(TimeZone.getDefault());

        return sdf.format(cal.getTime());
    }

// /**
//  * @deprecated 不鼓励使用。 返回当前时间串 格式:yyMMddhhmmss,在上传附件时使用
//  * 
//  * @return String
//  */
// public static String getCurDate() {
//  GregorianCalendar gcDate = new GregorianCalendar();
//  int year = gcDate.get(GregorianCalendar.YEAR);
//  int month = gcDate.get(GregorianCalendar.MONTH) + 1;
//  int day = gcDate.get(GregorianCalendar.DAY_OF_MONTH);
//  int hour = gcDate.get(GregorianCalendar.HOUR_OF_DAY);
//  int minute = gcDate.get(GregorianCalendar.MINUTE);
//  int sen = gcDate.get(GregorianCalendar.SECOND);
//  String y;
//  String m;
//  String d;
//  String h;
//  String n;
//  String s;
//  y = new Integer(year).toString();
//
//  if (month < 10) {
//   m = "0" + new Integer(month).toString();
//  } else {
//   m = new Integer(month).toString();
//  }
//
//  if (day < 10) {
//   d = "0" + new Integer(day).toString();
//  } else {
//   d = new Integer(day).toString();
//  }
//
//  if (hour < 10) {
//   h = "0" + new Integer(hour).toString();
//  } else {
//   h = new Integer(hour).toString();
//  }
//
//  if (minute < 10) {
//   n = "0" + new Integer(minute).toString();
//  } else {
//   n = new Integer(minute).toString();
//  }
//
//  if (sen < 10) {
//   s = "0" + new Integer(sen).toString();
//  } else {
//   s = new Integer(sen).toString();
//  }
//
//  return "" + y + m + d + h + n + s;
// }

    /**
     * 根据给定的格式，返回时间字符串。 和getFormatDate(String format)相似。
     *
     * @param format yyyy MM dd hh mm ss
     * @return 返回一个时间字符串
     */
    public static String getCurTimeByFormat(String format) {
        Date newdate = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(newdate);
    }

    /**
     * 获取两个时间串时间的差值，单位为秒
     *
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间 yyyy-MM-dd HH:mm:ss
     * @return 两个时间的差值(秒)
     */
    public static long getDiff(String startTime, String endTime) {
        long diff = 0;
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (startTime.length() == 7) {
            startTime = startTime + "-01 00:00:00";
        }
        if (endTime.length() == 7) {
            endTime = endTime + "-01 00:00:00";
        }
        if (startTime.length() == 10) {
            startTime = startTime + " 00:00:00";
        }
        if (endTime.length() == 10) {
            endTime = endTime + " 00:00:00";
        }
        try {
            Date startDate = ft.parse(startTime);
            Date endDate = ft.parse(endTime);
            diff = startDate.getTime() - endDate.getTime();
            diff = diff / 1000;
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return diff;
    }

    public static long getDiffAccountDate(String startAccountDate, String endAccountDate) {
        long diff = 0;
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM");
        try {
            Date startDate = ft.parse(startAccountDate);
            Date endDate = ft.parse(endAccountDate);
            diff = startDate.getTime() - endDate.getTime();
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return diff;
    }

    /**
     * 获取小时/分钟/秒
     *
     * @param second 秒
     * @return 包含小时、分钟、秒的时间字符串，例如3小时23分钟13秒。
     */
    public static String getHour(long second) {
        long hour = second / 60 / 60;
        long minute = (second - hour * 60 * 60) / 60;
        long sec = (second - hour * 60 * 60) - minute * 60;

        return null;
//          NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_pub", "01801pub_000042", null, new String[]{String.valueOf(hour),String.valueOf(minute),String.valueOf(sec)})/*{0}小时{1}分钟{2}秒*/;

    }

    /**
     * 返回指定时间字符串。
     * <p>
     * 格式：yyyy-MM-dd HH:mm:ss
     *
     * @return String 指定格式的日期字符串.
     */
    public static String getDateTime(long microsecond) {
        return getFormatDateTime(new Date(microsecond), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 返回当前时间加实数小时后的日期时间。
     * <p>
     * 格式：yyyy-MM-dd HH:mm:ss
     *
     * @return Float 加几实数小时.
     */
    public static String getDateByAddFltHour(float flt) {
        int addMinute = (int) (flt * 60);
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(GregorianCalendar.MINUTE, addMinute);
        return getFormatDateTime(cal.getTime(), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 返回指定时间加指定小时数后的日期时间。
     * <p>
     * 格式：yyyy-MM-dd HH:mm:ss
     *
     * @return 时间.
     */
    public static String getDateByAddHour(String datetime, int minute) {
        String returnTime = null;
        Calendar cal = new GregorianCalendar();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date;
        try {
            date = ft.parse(datetime);
            cal.setTime(date);
            cal.add(GregorianCalendar.MINUTE, minute);
            returnTime = getFormatDateTime(cal.getTime(), "yyyy-MM-dd HH:mm:ss");
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return returnTime;

    }

    /**
     * 获取两个时间串时间的差值，单位为小时
     *
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间 yyyy-MM-dd HH:mm:ss
     * @return 两个时间的差值(秒)
     */
    public static int getDiffHour(String startTime, String endTime) {
        long diff = 0;
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date startDate = ft.parse(startTime);
            Date endDate = ft.parse(endTime);
            diff = startDate.getTime() - endDate.getTime();
            diff = diff / (1000 * 60 * 60);
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return Long.valueOf(diff).intValue();
    }


    /**
     * 获取月份的下拉框
     *
     * @param selectName
     * @param value
     * @param hasBlank
     * @param js
     * @return 返回月份的下拉框。
     */
    public static String getMonthSelect(String selectName, String value,
                                        boolean hasBlank, String js) {
        StringBuffer sb = new StringBuffer("");
        sb.append("<select name=\"" + selectName + "\" " + js + ">");
        if (hasBlank) {
            sb.append("<option value=\"\"></option>");
        }
        for (int i = 1; i <= 12; i++) {
            if (StringUtils.isNotBlank(value) && i == Integer.parseInt(value)) {
                sb.append("<option value=\"" + i + "\" selected>" + i
                        + "</option>");
            } else {
                sb.append("<option value=\"" + i + "\">" + i + "</option>");
            }
        }
        sb.append("</select>");
        return sb.toString();
    }

    /**
     * 获取天的下拉框，默认的为1-31。 注意：此方法不能够和月份下拉框进行联动。
     *
     * @param selectName
     * @param value
     * @param hasBlank
     * @return 获得天的下拉框
     */
    public static String getDaySelect(String selectName, String value,
                                      boolean hasBlank) {
        StringBuffer sb = new StringBuffer("");
        sb.append("<select name=\"" + selectName + "\">");
        if (hasBlank) {
            sb.append("<option value=\"\"></option>");
        }
        for (int i = 1; i <= 31; i++) {
            if (StringUtils.isNotBlank(value) && i == Integer.parseInt(value)) {
                sb.append("<option value=\"" + i + "\" selected>" + i
                        + "</option>");
            } else {
                sb.append("<option value=\"" + i + "\">" + i + "</option>");
            }
        }
        sb.append("</select>");
        return sb.toString();
    }

    /**
     * 获取天的下拉框，默认的为1-31
     *
     * @param selectName
     * @param value
     * @param hasBlank
     * @param js
     * @return 获取天的下拉框
     */
    public static String getDaySelect(String selectName, String value,
                                      boolean hasBlank, String js) {
        StringBuffer sb = new StringBuffer("");
        sb.append("<select name=\"" + selectName + "\" " + js + ">");
        if (hasBlank) {
            sb.append("<option value=\"\"></option>");
        }
        for (int i = 1; i <= 31; i++) {
            if (StringUtils.isNotBlank(value) && i == Integer.parseInt(value)) {
                sb.append("<option value=\"" + i + "\" selected>" + i
                        + "</option>");
            } else {
                sb.append("<option value=\"" + i + "\">" + i + "</option>");
            }
        }
        sb.append("</select>");
        return sb.toString();
    }

    /**
     * 计算两天之间有多少个周末（这个周末，指星期六和星期天，一个周末返回结果为2，两个为4，以此类推。） （此方法目前用于统计司机用车记录。）
     * 注意开始日期和结束日期要统一起来。
     *
     * @param startDate 开始日期 ，格式"yyyy/MM/dd" 或者"yyyy-MM-dd"
     * @param endDate   结束日期 ，格式"yyyy/MM/dd"或者"yyyy-MM-dd"
     * @return int
     */
    public static int countWeekend(String startDate, String endDate) {
        int result = 0;
        Date sdate = null;
        Date edate = null;
        if (startDate.indexOf("/") > 0 && endDate.indexOf("/") > 0) {
            sdate = getDateObj(startDate, "/"); // 开始日期
            edate = getDateObj(endDate, "/");// 结束日期
        }
        if (startDate.indexOf("-") > 0 && endDate.indexOf("-") > 0) {
            sdate = getDateObj(startDate, "-"); // 开始日期
            edate = getDateObj(endDate, "-");// 结束日期
        }

        // 首先计算出都有那些日期，然后找出星期六星期天的日期
        int sumDays = Math.abs(getDiffDays(startDate, endDate));
        int dayOfWeek = 0;
        for (int i = 0; i <= sumDays; i++) {
            dayOfWeek = getDayOfWeek(getDateAdd(sdate, i)); // 计算每过一天的日期
            if (dayOfWeek == 1 || dayOfWeek == 7) { // 1 星期天 7星期六
                result++;
            }
        }
        return result;
    }

    /**
     * 返回两个日期之间相差多少天。
     * 注意开始日期和结束日期要统一起来。
     *
     * @param startDate 格式"yyyy/MM/dd" 或者"yyyy-MM-dd"
     * @param endDate   格式"yyyy/MM/dd" 或者"yyyy-MM-dd"
     * @return 整数。
     */
    public static int getDiffDays(String startDate, String endDate) {
        long diff = 0;
        SimpleDateFormat ft = null;
        if (startDate.indexOf("/") > 0 && endDate.indexOf("/") > 0) {
            ft = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        }
        if (startDate.indexOf("-") > 0 && endDate.indexOf("-") > 0) {
            ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        try {
            Date sDate = ft.parse(startDate + " 00:00:00");
            Date eDate = ft.parse(endDate + " 00:00:00");
            diff = eDate.getTime() - sDate.getTime();
            diff = diff / 86400000;// 1000*60*60*24;
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return (int) diff;

    }

    /**
     * 返回两个日期之间的详细日期数组(包括开始日期和结束日期)。 例如：2007/07/01 到2007/07/03 ,那么返回数组
     * {"2007/07/01","2007/07/02","2007/07/03"}
     * 注意开始日期和结束日期要统一起来。
     *
     * @param startDate 格式"yyyy/MM/dd"或者 yyyy-MM-dd
     * @param endDate   格式"yyyy/MM/dd"或者 yyyy-MM-dd
     * @return 返回一个字符串数组对象
     */
    public static String[] getArrayDiffDays(String startDate, String endDate) {
        int LEN = 0; // 用来计算两天之间总共有多少天
        // 如果结束日期和开始日期相同
        if (startDate.equals(endDate)) {
            return new String[]{startDate};
        }
        Date sdate = null;
        if (startDate.indexOf("/") > 0 && endDate.indexOf("/") > 0) {
            sdate = getDateObj(startDate, "/"); // 开始日期
        }
        if (startDate.indexOf("-") > 0 && endDate.indexOf("-") > 0) {
            sdate = getDateObj(startDate, "-"); // 开始日期
        }

        LEN = getDiffDays(startDate, endDate);
        String[] dateResult = new String[LEN + 1];
        dateResult[0] = startDate;
        for (int i = 1; i < LEN + 1; i++) {
            if (startDate.indexOf("/") > 0 && endDate.indexOf("/") > 0) {
                dateResult[i] = getFormatDateTime(getDateAdd(sdate, i),
                        "yyyy/MM/dd");
            }
            if (startDate.indexOf("-") > 0 && endDate.indexOf("-") > 0) {
                dateResult[i] = getFormatDateTime(getDateAdd(sdate, i),
                        "yyyy-MM-dd");
            }
        }

        return dateResult;
    }

    /**
     * 判断一个日期是否在开始日期和结束日期之间。
     *
     * @param srcDate   目标日期 yyyy/MM/dd 或者 yyyy-MM-dd
     * @param startDate 开始日期 yyyy/MM/dd 或者 yyyy-MM-dd
     * @param endDate   结束日期 yyyy/MM/dd 或者 yyyy-MM-dd
     * @return 大于等于开始日期小于等于结束日期，那么返回true，否则返回false
     */
    public static boolean isInStartEnd(String srcDate, String startDate,
                                       String endDate) {
        if (startDate.compareTo(srcDate) <= 0
                && endDate.compareTo(srcDate) >= 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取天的下拉框，默认的为1-4。 注意：此方法不能够和月份下拉框进行联动。
     *
     * @param selectName
     * @param value
     * @param hasBlank
     * @return 获得季度的下拉框
     */
    public static String getQuarterSelect(String selectName, String value,
                                          boolean hasBlank) {
        StringBuffer sb = new StringBuffer("");
        sb.append("<select name=\"" + selectName + "\">");
        if (hasBlank) {
            sb.append("<option value=\"\"></option>");
        }
        for (int i = 1; i <= 4; i++) {
            if (StringUtils.isNotBlank(value) && i == Integer.parseInt(value)) {
                sb.append("<option value=\"" + i + "\" selected>" + i
                        + "</option>");
            } else {
                sb.append("<option value=\"" + i + "\">" + i + "</option>");
            }
        }
        sb.append("</select>");
        return sb.toString();
    }

    /**
     * 获取季度的下拉框，默认的为1-4
     *
     * @param selectName
     * @param value
     * @param hasBlank
     * @param js
     * @return 获取季度的下拉框
     */
    public static String getQuarterSelect(String selectName, String value,
                                          boolean hasBlank, String js) {
        StringBuffer sb = new StringBuffer("");
        sb.append("<select name=\"" + selectName + "\" " + js + ">");
        if (hasBlank) {
            sb.append("<option value=\"\"></option>");
        }
        for (int i = 1; i <= 4; i++) {
            if (StringUtils.isNotBlank(value) && i == Integer.parseInt(value)) {
                sb.append("<option value=\"" + i + "\" selected>" + i
                        + "</option>");
            } else {
                sb.append("<option value=\"" + i + "\">" + i + "</option>");
            }
        }
        sb.append("</select>");
        return sb.toString();
    }

    /**
     * 将格式为yyyy或者yyyy.MM或者yyyy.MM.dd的日期转换为yyyy/MM/dd的格式。位数不足的，都补01。<br>
     * 例如.1999 = 1999/01/01 再如：1989.02=1989/02/01
     *
     * @param argDate 需要进行转换的日期。格式可能为yyyy或者yyyy.MM或者yyyy.MM.dd
     * @return 返回格式为yyyy/MM/dd的字符串
     */
    public static String changeDate(String argDate) {
        if (StringUtils.isBlank(argDate)) {
            return "";
        }
        String result = "";
        // 如果是格式为yyyy/MM/dd的就直接返回
        if (argDate.length() == 10 && argDate.indexOf("/") > 0) {
            return argDate;
        }
        String[] str = argDate.split("[.]"); // .比较特殊
        int LEN = str.length;
        for (int i = 0; i < LEN; i++) {
            if (str[i].length() == 1) {
                if ("0".equals(str[i])) {
                    str[i] = "01";
                } else {
                    str[i] = "0" + str[i];
                }
            }
        }
        if (LEN == 1) {
            result = argDate + "/01/01";
        }
        if (LEN == 2) {
            result = str[0] + "/" + str[1] + "/01";
        }
        if (LEN == 3) {
            result = str[0] + "/" + str[1] + "/" + str[2];
        }
        return result;
    }

    /**
     * 将格式为yyyy或者yyyy.MM或者yyyy.MM.dd的日期转换为yyyy/MM/dd的格式。位数不足的，都补01。<br>
     * 例如.1999 = 1999/01/01 再如：1989.02=1989/02/01
     *
     * @param argDate 需要进行转换的日期。格式可能为yyyy或者yyyy.MM或者yyyy.MM.dd
     * @return 返回格式为yyyy/MM/dd的字符串
     */
    public static String changeDateWithSplit(String argDate, String split) {
        if (StringUtils.isBlank(argDate)) {
            return "";
        }
        if (StringUtils.isBlank(split)) {
            split = "-";
        }
        String result = "";
        // 如果是格式为yyyy/MM/dd的就直接返回
        if (argDate.length() == 10 && argDate.indexOf("/") > 0) {
            return argDate;
        }
//   如果是格式为yyyy-MM-dd的就直接返回
        if (argDate.length() == 10 && argDate.indexOf("-") > 0) {
            return argDate;
        }
        String[] str = argDate.split("[.]"); // .比较特殊
        int LEN = str.length;
        for (int i = 0; i < LEN; i++) {
            if (str[i].length() == 1) {
                if ("0".equals(str[i])) {
                    str[i] = "01";
                } else {
                    str[i] = "0" + str[i];
                }
            }
        }
        if (LEN == 1) {
            result = argDate + split + "01" + split + "01";
        }
        if (LEN == 2) {
            result = str[0] + split + str[1] + split + "01";
        }
        if (LEN == 3) {
            result = str[0] + split + str[1] + split + str[2];
        }
        return result;
    }

    /**
     * 返回指定日期的的下一个月的天数。
     *
     * @param argDate 格式为yyyy-MM-dd或者yyyy/MM/dd
     * @return 下一个月的天数。
     */
    public static int getNextMonthDays(String argDate) {
        String[] temp = null;
        if (argDate.indexOf("/") > 0) {
            temp = argDate.split("/");
        }
        if (argDate.indexOf("-") > 0) {
            temp = argDate.split("-");
        }
        Calendar cal = new GregorianCalendar(new Integer(temp[0]).intValue(),
                new Integer(temp[1]).intValue() - 1, new Integer(temp[2]).intValue());
        int curMonth = cal.get(Calendar.MONTH);
        cal.set(Calendar.MONTH, curMonth + 1);

        int curyear = cal.get(Calendar.YEAR);// 当前年份
        curMonth = cal.get(Calendar.MONTH);// 当前月份,0-11

        int mArray[] = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30,
                31};
        // 判断闰年的情况 ，2月份有29天；
        if ((curyear % 400 == 0)
                || ((curyear % 100 != 0) && (curyear % 4 == 0))) {
            mArray[1] = 29;
        }
        return mArray[curMonth];
    }

    public static void main(String[] args) {
//  System.out.println(DateUtil.getCurrentDateTime());
//  System.out.println("first="+changeDateWithSplit("2000.1",""));
//  System.out.println("second="+changeDateWithSplit("2000.1","/"));
//  String[] t = getArrayDiffDays("2008/02/15","2008/02/19");
//  for(int i=0;i<t.length;i++){
//   System.out.println(t[i]) ;
//  }
//  t = getArrayDiffDays("2008-02-15","2008-02-19");
//  for(int i=0;i<t.length;i++){
//   System.out.println(t[i]) ;
//  }
//  System.out.println(getNextMonthDays("2008/02/15") + "||"+getCurrentMonth() + "||"
//    + DateUtil.changeDate("1999"));
//  System.out.println(DateUtil.changeDate("1999.1"));
//  System.out.println(DateUtil.changeDate("1999.11"));
//  System.out.println(DateUtil.changeDate("1999.1.2"));
//  System.out.println(DateUtil.changeDate("1999.11.12"));
    }

    /**
     * 通过月份获取当前所在季度的第一个月份
     */
    public static int getFirstMonthCurrentQuarter(int month) {
        if ((month < 1) || (month > 12)) {
            return 1;
        }
        int a = month / 3;
        double b = month % 3;
        if (b > 0) {
            a = a + 1;
        }
        if (a == 1) {
            return 1;
        } else if (a == 2) {
            return 4;
        } else if (a == 3) {
            return 7;
        } else {
            return 10;
        }
    }

    /**
     * 返回指定时间加指定月份间隔后的日期时间。
     * <p>
     * 格式：yyyy-MM-dd
     *
     * @return 时间.
     */
    public static String getDateByAddMonth(String datetime, int month) {
        String returnTime = null;
        Calendar cal = new GregorianCalendar();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
        Date date;
        try {
            date = ft.parse(datetime);
            cal.setTime(date);
            cal.add(GregorianCalendar.MONTH, month);
            returnTime = getFormatDateTime(cal.getTime(), "yyyy-MM-dd");
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return returnTime;

    }

    public String GetDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String sDate = sdf.format(new Date());
        return sDate;
    }

    /**
     * @return String
     * @see （格式为：yyy-MM-dd HH:mm:ss）
     */
    public static String GetDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sDate = sdf.format(new Date());
        return sDate;
    }

    /**
     * 按指定格式取得当前时间()
     *
     * @return String
     * @see
     */
    public String GetTimeFormat(String strFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(strFormat);
        String sDate = sdf.format(new Date());
        return sDate;
    }

    /**
     * 取得指定时间的给定格式()
     *
     * @return String
     * @throws ParseException
     * @see
     */
    public String SetDateFormat(String myDate, String strFormat) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(strFormat);
        String sDate = sdf.format(sdf.parse(myDate));
        return sDate;
    }

    public String FormatDateTime(String strDateTime, String strFormat) {
        String sDateTime = strDateTime;
        try {
            Calendar Cal = parseDateTime(strDateTime);
            SimpleDateFormat sdf = null;
            sdf = new SimpleDateFormat(strFormat);
            sDateTime = sdf.format(Cal.getTime());
        } catch (Exception e) {
        }
        return sDateTime;
    }

    public static Calendar parseDateTime(String baseDate) {
        Calendar cal = null;
        cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int yy = Integer.parseInt(baseDate.substring(0, 4));
        int mm = Integer.parseInt(baseDate.substring(5, 7)) - 1;
        int dd = Integer.parseInt(baseDate.substring(8, 10));
        int hh = 0;
        int mi = 0;
        int ss = 0;
        if (baseDate.length() > 12) {
            hh = Integer.parseInt(baseDate.substring(11, 13));
            mi = Integer.parseInt(baseDate.substring(14, 16));
            ss = Integer.parseInt(baseDate.substring(17, 19));
        }
        cal.set(yy, mm, dd, hh, mi, ss);
        return cal;
    }

    public static int getDay(String strDate) {
        Calendar cal = parseDateTime(strDate);
        return cal.get(Calendar.DATE);
    }

    public static int getMonth(String strDate) {
        Calendar cal = parseDateTime(strDate);
        return cal.get(Calendar.MONTH) + 1;
    }

    public static int getWeekDay(String strDate) {
        Calendar cal = parseDateTime(strDate);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    public static int getDbWeekDay(String strDate) {
        Calendar cal = parseDateTime(strDate);
        return ((int) (cal.get(Calendar.WEEK_OF_YEAR) + 1) / 2);
    }

    public static int getQuarter(String strDate) {
        int season = 0;
        Calendar c = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date curDate = format.parse(strDate);
            c.setTime(curDate);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        int month = c.get(Calendar.MONTH);
        switch (month) {
            case Calendar.JANUARY:
            case Calendar.FEBRUARY:
            case Calendar.MARCH:
                season = 1;
                break;
            case Calendar.APRIL:
            case Calendar.MAY:
            case Calendar.JUNE:
                season = 2;
                break;
            case Calendar.JULY:
            case Calendar.AUGUST:
            case Calendar.SEPTEMBER:
                season = 3;
                break;
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
            case Calendar.DECEMBER:
                season = 4;
                break;
            default:
                break;
        }
        return season;
    }


    public static int getYear(String strDate) {
        Calendar cal = parseDateTime(strDate);
        return cal.get(Calendar.YEAR) + 1900;
    }

    public String DateAdd(String strDate, int iCount, int iType) {
        Calendar Cal = parseDateTime(strDate);
        int pType = 0;
        if (iType == 0) {
            pType = 1;
        } else if (iType == 1) {
            pType = 2;
        } else if (iType == 2) {
            pType = 5;
        } else if (iType == 3) {
            pType = 10;
        } else if (iType == 4) {
            pType = 12;
        } else if (iType == 5) {
            pType = 13;
        }
        Cal.add(pType, iCount);
        SimpleDateFormat sdf = null;
        if (iType <= 2)
            sdf = new SimpleDateFormat("yyyy-MM-dd");
        else
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sDate = sdf.format(Cal.getTime());
        return sDate;
    }

    // public String DateAdd(String strOption, int iDays, String strStartDate)
// {
//     if(!strOption.equals( "d"));
//     return strStartDate;
// }
    public int DateDiff(String strDateBegin, String strDateEnd, int iType) {
        Calendar calBegin = parseDateTime(strDateBegin);
        Calendar calEnd = parseDateTime(strDateEnd);
        long lBegin = calBegin.getTimeInMillis();
        long lEnd = calEnd.getTimeInMillis();
        int ss = (int) ((lBegin - lEnd) / 1000L);
        int min = ss / 60;
        int hour = min / 60;
        int day = hour / 24;
        if (iType == 0)
            return hour;
        if (iType == 1)
            return min;
        if (iType == 2)
            return day;
        else
            return -1;
    }

    /*****************************************
     * @功能 判断某年是否为闰年
     * @return boolean
     * @throws ParseException
     ****************************************/
    public boolean isLeapYear(int yearNum) {
        boolean isLeep = false;
        /**判断是否为闰年，赋值给一标识符flag*/
        if ((yearNum % 4 == 0) && (yearNum % 100 != 0)) {
            isLeep = true;
        } else if (yearNum % 400 == 0) {
            isLeep = true;
        } else {
            isLeep = false;
        }
        return isLeep;
    }

    /*****************************************
     * @功能 计算当前日期某年的第几周
     * @return interger
     * @throws ParseException
     ****************************************/
    public int getWeekNumOfYear() {
        Calendar calendar = Calendar.getInstance();
        int iWeekNum = calendar.get(Calendar.WEEK_OF_YEAR);
        return iWeekNum;
    }

    /*****************************************
     * @功能 计算指定日期某年的第几周
     * @return interger
     * @throws ParseException
     ****************************************/
    public int getWeekNumOfYearDay(String strDate) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date curDate = format.parse(strDate);
        calendar.setTime(curDate);
        int iWeekNum = calendar.get(Calendar.WEEK_OF_YEAR);
        return iWeekNum;
    }

    /*****************************************
     * @功能 计算某年某周的开始日期
     * @return interger
     * @throws ParseException
     ****************************************/
    public String getYearWeekFirstDay(int yearNum, int weekNum) throws ParseException {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, yearNum);
        cal.set(Calendar.WEEK_OF_YEAR, weekNum);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        //分别取得当前日期的年、月、日
        String tempYear = Integer.toString(yearNum);
        String tempMonth = Integer.toString(cal.get(Calendar.MONTH) + 1);
        String tempDay = Integer.toString(cal.get(Calendar.DATE));
        String tempDate = tempYear + "-" + tempMonth + "-" + tempDay;
        return SetDateFormat(tempDate, "yyyy-MM-dd");


    }

    /*****************************************
     * @功能 计算某年某周的结束日期
     * @return interger
     * @throws ParseException
     ****************************************/
    public String getYearWeekEndDay(int yearNum, int weekNum) throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, yearNum);
        cal.set(Calendar.WEEK_OF_YEAR, weekNum + 1);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        //分别取得当前日期的年、月、日
        String tempYear = Integer.toString(yearNum);
        String tempMonth = Integer.toString(cal.get(Calendar.MONTH) + 1);
        String tempDay = Integer.toString(cal.get(Calendar.DATE));
        String tempDate = tempYear + "-" + tempMonth + "-" + tempDay;
        return SetDateFormat(tempDate, "yyyy-MM-dd");
    }


    /*****************************************
     * @功能 计算某年某月的开始日期
     * @return interger
     * @throws ParseException
     ****************************************/
    public String getYearMonthFirstDay(int yearNum, int monthNum) throws ParseException {

        //分别取得当前日期的年、月、日
        String tempYear = Integer.toString(yearNum);
        String tempMonth = Integer.toString(monthNum);
        String tempDay = "1";
        String tempDate = tempYear + "-" + tempMonth + "-" + tempDay;
        return SetDateFormat(tempDate, "yyyy-MM-dd");

    }

    /*****************************************
     * @功能 计算某年某月的结束日期
     * @return interger
     * @throws ParseException
     ****************************************/
    public String getYearMonthEndDay(int yearNum, int monthNum) throws ParseException {

        //分别取得当前日期的年、月、日
        String tempYear = Integer.toString(yearNum);
        String tempMonth = Integer.toString(monthNum);
        String tempDay = "31";
        switch (Integer.valueOf(tempMonth)) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                tempDay = "31";
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                tempDay = "30";
                break;
            case 2:
                if (isLeapYear(yearNum)) {
                    tempDay = "29";
                } else {
                    tempDay = "28";
                }
                break;
            default:
                break;
        }
        //System.out.println("tempDay:" + tempDay);
        String tempDate = tempYear + "-" + tempMonth + "-" + tempDay;
        return SetDateFormat(tempDate, "yyyy-MM-dd");
    }

    public static ArrayList<String> getAllDay(int year) {
        ArrayList<String> dateStr = new ArrayList<String>();
        try {
            Calendar ca = Calendar.getInstance();
            ca.setTime((new SimpleDateFormat("yyyy")).parse(year + ""));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            while (true) {
                dateStr.add(sdf.format(ca.getTime()));
                ca.add(Calendar.DAY_OF_YEAR, 1);
                if (ca.get(Calendar.YEAR) > year)
                    break;
            }
        } catch (ParseException e) {
//	        log.error(e.getMessage());
            log.error(e.getMessage());
        }
        return dateStr;

    }

    /**
     * @return interger
     * @throws
     * @功能 计算传入日期是该月的第几周（如果一个周同时属于两个相邻的月份，那么这个周为后面月份的第一周）
     */
    public static int getMonthOfWeek(int y, int m, int d) {

        Calendar cal = Calendar.getInstance();
        // 设置周一为每星期的第一天
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        cal.clear();
        cal.set(Calendar.YEAR, y);
        cal.set(Calendar.MONTH, m - 1);

        // 获取该月内的周数
        int weekNum = cal.getActualMaximum(Calendar.WEEK_OF_MONTH);
        // 获取该月内的天数
        int dayNum = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        // 当前日期是该月内的第几周
        cal.set(Calendar.DAY_OF_MONTH, d);
        int currWeekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);

        // 只需要考虑每个月的最后7天
        if (d > dayNum - 7) {
            // 如果该月倒数第7天是周一，返回当前currWeekOfMonth
            if (getDayOfWeek(String.valueOf(y), String.valueOf(m), String.valueOf(dayNum - 6)) == 2) {
                return currWeekOfMonth;
            }
            // 如果不是该月最后一周，直接返回
            if (currWeekOfMonth < weekNum) {
                return currWeekOfMonth;
            } else {
                return 1;
            }
        }

        return currWeekOfMonth;
    }

    /**
     * 以周日为每周开始的第一天
     *
     * @param strDate
     * @return
     */
    public static int getWeekDay2(String strDate) {
        Calendar cal = parseDateTime2(strDate);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    public static Calendar parseDateTime2(String baseDate) {
        Calendar cal = null;
        cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.SUNDAY);
        int yy = Integer.parseInt(baseDate.substring(0, 4));
        int mm = Integer.parseInt(baseDate.substring(5, 7)) - 1;
        int dd = Integer.parseInt(baseDate.substring(8, 10));
        int hh = 0;
        int mi = 0;
        int ss = 0;
        if (baseDate.length() > 12) {
            hh = Integer.parseInt(baseDate.substring(11, 13));
            mi = Integer.parseInt(baseDate.substring(14, 16));
            ss = Integer.parseInt(baseDate.substring(17, 19));
        }
        cal.set(yy, mm, dd, hh, mi, ss);
        return cal;
    }

}
