package com.crazymen.freenote.util;

import java.util.Locale;
import java.util.Random;

/**
 * 周报里的鼓励语：按「这一周记录了多少天」分四档，共 100 条互不重复。
 *
 * - 0 天：不催、不评判，留一个轻的入口；
 * - 1–2 天：认可「回来过」这件事；
 * - 3–5 天：稳住的节奏，鼓励继续；
 * - 6–7 天：把这份坚持说出来。
 *
 * 文案里可以带 %d，会替换成实际记录天数。
 */
public final class Encouragements {

    private static final Random RANDOM = new Random();

    /** 0 天：这周一个字都没写 */
    private static final String[] IDLE = {
            "空白也是记录的一部分。今晚写一句，给这周留个句点。",
            "不用等有心情才写。写一句，心情会跟上来。",
            "想记就记，不必完整。",
            "这周本子上还是空的。空着也没关系，它一直在这儿。",
            "一周没写，说明你在忙着过日子。忙完再回来。",
            "没写下的日子，也照样算你的。",
            "从一句开始就好，一个字也行。",
            "这周没有字迹，但你并没有缺席。",
            "有些星期适合记，有些星期适合过。",
            "打开这一页，就已经是开始了。",
            "不用补写过去。写现在这一句就够了。",
            "记录是给未来的自己留一盏灯，今天点上也不晚。",
            "一周很长，一句很短。短的更容易做到。",
            "空白不是失败，是还没到想说话的时候。",
            "这周先欠自己一句，随时可以还。",
            "不必一次写清一整周，写清此刻就好。",
            "手边有什么就记什么：一杯茶，一阵风。",
            "想起了什么，就写那一句。",
            "没记录的这一周，也值得被记得。",
            "明天写，也算数。",
            "日子过了就过了，字还可以补。",
            "本子不催你，我也不会。",
            "这周辛苦了，哪怕什么都没写下。",
            "写不写，你都是完整的。",
            "下次翻开，从最简单的一行开始。"
    };

    /** 1–2 天：零星几句 */
    private static final String[] SPARSE = {
            "这周你来了 %d 天。不用每天写，能回来就好。",
            "零星几句也算数，它们会替你记住这一周。",
            "下次想起来的时候，再写一句就好。",
            "两次落笔，也比一次不写多了一点。",
            "写得少，不代表记得少。",
            "这几行字，就是这周的小小锚点。",
            "有空才写，也是一种诚实。",
            "你的节奏不用和别人比。",
            "一周里能想起这件事两次，已经不容易。",
            "短句也可以很沉。",
            "这周的字不多，但都是真的。",
            "写下来的时候，你确实在场。",
            "零碎的时间，攒成了这几行。",
            "不必连着写，断开也没关系。",
            "一周写两天，一年也有一百次回头。",
            "这几句会替你把那几天留住。",
            "有点忙，还能写，已经很好了。",
            "写一两句，是给自己留的余地。",
            "慢一点写，句子会更像自己。",
            "数量不重要，落笔的那一刻才重要。",
            "这周留下了痕迹，浅浅的，也是痕迹。",
            "下次打开，试着多写一句。",
            "不追求完整，只追求真实。",
            "你已经开始了，剩下的交给时间。",
            "这几天写下的，别小看它们。"
    };

    /** 3–5 天：节奏稳住了 */
    private static final String[] STEADY = {
            "这周有 %d 天留下字迹，节奏刚刚好。",
            "记录已经成了习惯。别急着写得更好，先写得更多。",
            "每次打开，都是一次小小的归来。",
            "一半的日子有字，一半的日子有生活。",
            "这个频率很稳，继续就好。",
            "你开始记得自己了。",
            "一周过半都写了，这份稳定很珍贵。",
            "写得不急，但一直在。",
            "这几天的记录连起来，就是一条小路。",
            "有几天没写，也没影响这周的完整。",
            "你在给自己留证据：这些日子真实存在过。",
            "习惯不是每天，是想起来就会来。",
            "这周的字，有点像日记，也有点像聊天。",
            "稳定比用力更难得。",
            "每次落笔，都是在照顾自己。",
            "四五天，足够看清这一周的形状。",
            "写得越来越顺了，别停下来。",
            "有些天是记录，有些天是休息，都对。",
            "你在建立一个只属于自己的地方。",
            "这周的节奏，值得保持。",
            "状态好的时候多写两句，不好就少写。",
            "你已经不需要提醒了。",
            "一周写几天，日子就有了刻度。",
            "这几天写下的，以后会感谢。",
            "保持这个温度就好。"
    };

    /** 6–7 天：几乎写满 */
    private static final String[] DENSE = {
            "几乎每天都写了，这份坚持很珍贵。",
            "一整周都在记录，回头翻会很有分量。",
            "写下来的日子，才是真正属于自己的日子。",
            "七天里没有断过，这很不容易。",
            "这周的你，被完整地留下来了。",
            "每天都打开一次，说明它真的有用。",
            "一周的密度，以后就是一本小书。",
            "坚持到这里，已经不只是习惯了。",
            "这周没有漏掉自己。",
            "每天一句，凑成了一整周。",
            "你的记录开始有厚度了。",
            "一周不断，靠的不是意志，是在意。",
            "这样的周，攒到年底会很惊人。",
            "天天都写，也别忘了偶尔歇一天。",
            "这一周的每一天都有落点。",
            "你把自己照顾得很好。",
            "记录成了日常，比任何目标都可靠。",
            "一周写满，是给自己的一份礼物。",
            "这些字，以后会替你回到这一周。",
            "稳定到第七天，值得为自己骄傲一次。",
            "每天都留下一点，日子就不算白过。",
            "这周的完整，来自每天那几分钟。",
            "写满一周，说明你在认真生活。",
            "不用写得多，你已经写得很久。",
            "这七天，一天都没少。"
    };

    private static final String[][] POOLS = {IDLE, SPARSE, STEADY, DENSE};

    private Encouragements() {
    }

    /** 4 档，共 100 条 */
    public static int total() {
        int sum = 0;
        for (String[] pool : POOLS) {
            sum += pool.length;
        }
        return sum;
    }

    /** 记录天数 → 档位下标（0..3） */
    public static int tierOf(int days) {
        if (days <= 0) {
            return 0;
        }
        if (days <= 2) {
            return 1;
        }
        if (days <= 5) {
            return 2;
        }
        return 3;
    }

    /** 某一档的全部文案（副本，避免外部改动常量） */
    public static String[] pool(int days) {
        return POOLS[tierOf(days)].clone();
    }

    public static String[] poolByTier(int tier) {
        return POOLS[Math.max(0, Math.min(POOLS.length - 1, tier))].clone();
    }

    /** 把 %d 换成实际天数 */
    public static String decorate(String line, int days) {
        if (line == null) {
            return "";
        }
        return line.contains("%d") ? String.format(Locale.CHINA, line, days) : line;
    }

    /** 随机取一条，尽量避开上一句（用于「每次打开都不一样」） */
    public static String random(int days, String avoid) {
        String[] pool = POOLS[tierOf(days)];
        if (pool.length == 1) {
            return decorate(pool[0], days);
        }
        for (int i = 0; i < 8; i++) {
            String candidate = pool[RANDOM.nextInt(pool.length)];
            String text = decorate(candidate, days);
            if (!text.equals(avoid)) {
                return text;
            }
        }
        // 极小概率连续撞上：换下一条，保证一定不同
        for (String candidate : pool) {
            String text = decorate(candidate, days);
            if (!text.equals(avoid)) {
                return text;
            }
        }
        return decorate(pool[0], days);
    }

    /** 取当前句子的下一条（「换一句」用，保证与当前不同） */
    public static String next(int days, String current) {
        String[] pool = POOLS[tierOf(days)];
        int start = 0;
        String decoratedCurrent = current == null ? "" : current;
        for (int i = 0; i < pool.length; i++) {
            if (decorate(pool[i], days).equals(decoratedCurrent)) {
                start = (i + 1) % pool.length;
                break;
            }
        }
        String candidate = decorate(pool[start], days);
        if (candidate.equals(decoratedCurrent)) {
            candidate = decorate(pool[(start + 1) % pool.length], days);
        }
        return candidate;
    }
}
