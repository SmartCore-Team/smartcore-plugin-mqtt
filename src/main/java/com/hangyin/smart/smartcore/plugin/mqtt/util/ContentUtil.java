package com.hangyin.smart.smartcore.plugin.mqtt.util;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 * @author hang.yin
 * @date 2020-01-06
 */
public class ContentUtil {

    @SuppressWarnings("RegExpRedundantEscape")
    private static List<String> getBraceContent(String msg) {
        List<String> list = new LinkedList<>();
        Pattern p = Pattern.compile("(\\{[^\\}]*\\})");
        Matcher m = p.matcher(msg);
        while(m.find()){
            list.add(m.group().substring(1, m.group().length()-1));
        }
        return list;
    }

    public static String getContent(String str, Map<String, Object> params) {
        str = encode(str);
        List<String> replaceList = ContentUtil.getBraceContent(str);
        for(String replaceEl: replaceList) {
            if(replaceEl.startsWith("\"") && replaceEl.endsWith("\"")) {
                String replaceEl2 = replaceEl;
                replaceEl2 = StringUtils.replace(replaceEl2,"\"", "");
                String value = String.valueOf(params.get(replaceEl2));
                if(params.get(replaceEl2) instanceof String) {
                    str = str.replace("{" + replaceEl + "}", "\"" + value + "\"");
                } else {
                    str = str.replace("{" + replaceEl + "}", value);
                }
            } else {
                String value = String.valueOf(params.get(replaceEl));
                str = str.replace("{" + replaceEl + "}", value);
            }
        }
        str = decode(str);
        return str;
    }

    private static String encode(String str) {
        str = StringUtils.replace(str, "\\{", "[ENCODE]%7b[/ENCODE]");
        str = StringUtils.replace(str, "\\}", "[ENCODE]%7d[/ENCODE]");
        return str;
    }

    private static String decode(String str) {
        str = StringUtils.replace(str, "[ENCODE]%7b[/ENCODE]", "{");
        str = StringUtils.replace(str, "[ENCODE]%7d[/ENCODE]", "}");
        return str;
    }

//    public static List<String> getDeviceId(List<String> deviceIds) {
//        Set<String> rDeviceIds = new HashSet<>();
//        deviceIds.forEach(d -> {
//            String patternString = createRegexFromGlob(d);
//            DeviceContext.devices.keySet().forEach(dd -> {
//                if(dd.matches(patternString)) {
//                    rDeviceIds.add(dd);
//                }
//            });
//        });
//
//        return new ArrayList<>(rDeviceIds);
//    }
//
//    private static String createRegexFromGlob(String glob) {
//        StringBuilder out = new StringBuilder("^");
//        for(int i = 0; i < glob.length(); ++i) {
//            final char c = glob.charAt(i);
//            switch(c) {
//                case '*': out.append(".*"); break;
//                case '?': out.append('.'); break;
//                case '.': out.append("\\."); break;
//                case '\\': out.append("\\\\"); break;
//                default: out.append(c);
//            }
//        }
//        out.append('$');
//        return out.toString();
//    }

}
