import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        //try {
        final FileHandler fileHandler = new FileHandler();
        final JFileChooser file = fileHandler.openPopup();
        final JFileChooser fileToCompare = fileHandler.openPopup();
        Map<String, String> bean = loadFileAndMapIntoBean(fileHandler, file);
        Map<String, String> beanToCompare = loadFileAndMapIntoBean(fileHandler, fileToCompare);
        analyzeAndExportBean(bean, beanToCompare, file, fileHandler);
        JOptionPane.showMessageDialog(new JFrame(), "Analisi conclusa correttamente.", "Dialog", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
        //} catch (Exception exception) {
        //    JOptionPane.showMessageDialog(new JFrame(), "Errore interno", "Error", JOptionPane.ERROR_MESSAGE);
        //    System.exit(0);
        //}
    }

    private static void analyzeAndExportBean(Map<String, String> bean, Map<String, String> beanToCompare, JFileChooser directory, FileHandler fileHandler)
            throws IOException {
        Map<String, String> finalMismatches = new HashMap<>();
        Map<String, Map<String, String>> finalMismatchesByFile = new HashMap<>();
        Map<String, Integer> counterMismatches = new HashMap<>();
        String[] headerToUse = bean.get("HEADER").split(";");
        String[] headerToCompare = beanToCompare.get("HEADER").split(";");
        int headerLobIdIndex = -1;
        for (int i = 0; i < headerToUse.length; i++) {
            if (headerToUse[i].equals("lob_id")) {
                headerLobIdIndex = i;
                break;
            }
        }
        boolean isMovement = directory.getSelectedFile().getName().contains("Movement");
        for (Map.Entry<String, String> row : bean.entrySet()) {
            String[] value = row.getValue().split(";", -1);
            if (beanToCompare.get(row.getKey()) != null) {
                String[] valueToCompare = beanToCompare.get(row.getKey()).split(";", -1);
                for (int i = 0; i < value.length; i++) {
                    boolean doCompare = true;
                    String headerId = headerToUse[i];
                    int index = findHeaderId(headerId, headerToCompare);
                    if (index != -1) {//If header not find skip value
                        if (headerId.equals("token_snapshot") || headerId.equals("date_entd") || headerId.equals("token_movement") || headerId.equals("token_current_snapshot")
                                || headerId.equals("token_previous_snapshot") /*|| headerId.equals("user_entd") || headerId.equals(
                            "token_lob_container") || headerId.equals("token_exhibit_b1_lob") || headerId.equals("token_report_b1") || headerId.equals(
                            "token_pool_settlement_b1") || headerId.equals("token_cash_settlement_b1")*/) {
                            doCompare = false;
                        }
                        //                        if (headerId.equals("lives")) {
                        //                            doCompare = false;
                        //                        }
                        //                        if (value[i].equals("[NULL]")) {
                        //                            value[i] = "";
                        //                        }
                        //                        if (valueToCompare[index].equals("[NULL]")) {
                        //                            valueToCompare[index] = "";
                        //                        }
                        //                        if (headerId.equals("insr_pct")) {
                        //                            if (!valueToCompare[headerLobIdIndex].equals("4")) {
                        //                                doCompare = false;
                        //                            }
                        //                        }
                        /*
                        if (headerId.equals("lives")) {
                            doCompare = false;
                        }

                        if (headerId.equals("pool_exch_rate")) {
                            doCompare = false;
                        }
                        if (headerId.equals("cash_exch_rate")) {
                            doCompare = false;
                        }
                        if (headerId.equals("cash_owed")) {
                            doCompare = false;
                        }
                        if (headerId.equals("rein_cd1_pct")) {
                            doCompare = false;
                        }
                        if (headerId.equals("ier_run_date")) {
                            doCompare = false;
                        }
                        if (headerId.equals("def_reim_fac")) {
                            doCompare = false;
                        }
                        if (headerId.equals("pool_owed") || headerId.equals("cash_paid_us") || headerId.equals("cash_paid") || headerId.equals("plpd")
                            || headerId.equals("cshpd")) {
                            doCompare = false;
                        }

                        //MOVEMENT
                        if (headerId.equals("prv_ins_pct")) {
                            if (!valueToCompare[headerLobIdIndex].equals("41") && !valueToCompare[headerLobIdIndex].equals("43")) {
                                doCompare = false;
                            }
                        }
                        if (headerId.equals("cur_ins_pct")) {
                            if (!valueToCompare[headerLobIdIndex].equals("41") && !valueToCompare[headerLobIdIndex].equals("43")) {
                                doCompare = false;
                            }
                        }
                        if (headerId.equals("seq")) {
                            doCompare = false;
                        }

                        //                        if (headerId.equals("prv_def_reim")) {
                        //                            doCompare = false;
                        //                        }
                        //                        if (headerId.equals("cur_def_reim")) {
                        //                            doCompare = false;
                        //                        }
                        //
                        //                        //FxRate Problems
                        //                        if (headerId.equals("fxc_re_us") || headerId.equals("fxc_rc_us") || headerId.equals("chg_eri_us") || headerId.equals("chg_prem_us")
                        //                            || headerId.equals("cur_mth_rte") || headerId.equals("vol_re_us") || headerId.equals("vol_rc_us") || headerId.equals(
                        //                            "cur_prem_us")) {
                        //                            doCompare = false;
                        //                        }
                        //
                        //                        if (headerId.equals("cur_eri_lc") || headerId.equals("chg_eri_lc") || headerId.equals("cur_eri_us") || headerId.equals("cur_re_lc")
                        //                            || headerId.equals("chg_re_lc")) {
                        //                            doCompare = false;
                        //                        }

                        if (headerId.equals("def_reim_fac") || headerId.equals("prv_def_reim") || headerId.equals("cur_def_reim") || headerId.equals(
                            "prv_ier_rte") || headerId.equals("prv_csh_rte") || headerId.equals("cur_csh_rte")|| headerId.equals("prv_pl_rte")) {
                            if (value[i].equals("")) {
                                value[i] = "0";
                            }
                            if (valueToCompare[index].equals("")) {
                                valueToCompare[index] = "0";
                            }
                        }

                         */
                        if (doCompare && !value[i].equals(valueToCompare[index])) {
                            if (Arrays.asList("paid_prem", "re_prem", "inv_incm", "clms", "comm", "taxes_incl", "chg_in_resv", "lc_div", "ie_expense",
                                    "risk_charge", "re_risk", "tot_mgn_def", "begn_iab", "iab_int_amt", "to_from_othr_pol", "ier_iab", "re_deficit",
                                    "rein_sett_due", "pool_total", "pool_sett_due", "cash_owed", "pool_owed", "cash_paid", "pool_paid", "cash_paid_us",
                                    "pool_paid_us", "div_amt_paid", "div_amt_owed", "re_prem_usd", "eri_usd", "fxc_re_us", "chg_eri_us", "cur_eri_us", "prv_eri_lc",
                                    "cur_eri_lc", "chg_eri_lc", "prv_eri_us", "cur_eri_us", "chg_eri_us", "prv_re_lc", "cur_re_lc", "chg_re_lc", "prv_rc_lc",
                                    "cur_rc_lc", "chg_rc_lc", "vol_re_us", "fxc_re_us", "vol_rc_us", "fxc_rc_us", "paid_re_lc", "paid_re_us", "paid_rc_lc",
                                    "paid_rc_us", "paid_ndc_lc", "paid_ndc_us", "pool_rec_lc", "pool_rec_us", "prv_prem_lc", "cur_prem_lc", "chg_prem_lc",
                                    "prv_prem_us", "cur_prem_us", "chg_prem_us").contains(headerId) || Arrays.asList("ier_exch_rate", "cash_exch_rate",
                                            "pool_exch_rate", "mth_exch_rate", "cur_mth_rte", "cur_pl_rte", "prv_csh_rte", "cur_csh_rte", "prv_pl_rte")
                                    .contains(headerId)) {
                                if (value[i].equals("")) {
                                    value[i] = "0";
                                }
                                if (valueToCompare[index].equals("")) {
                                    valueToCompare[index] = "0";
                                }
                                BigDecimal number = null;
                                BigDecimal numberToCompare = null;
                                if (Arrays.asList("ier_exch_rate", "cash_exch_rate", "pool_exch_rate", "mth_exch_rate").contains(headerId)) {
                                    try {
                                        number = new BigDecimal(value[i]).setScale(6, RoundingMode.HALF_UP);
                                        numberToCompare = new BigDecimal(valueToCompare[index]).setScale(6, RoundingMode.HALF_UP);
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                    if (number.subtract(numberToCompare).abs().compareTo(new BigDecimal("0.000001")) > 0) {
                                        String result;
                                        Map<String, String> byHeader;
                                        if (finalMismatches.containsKey(row.getKey())) {
                                            result =
                                                    finalMismatches.get(row.getKey()) + " + " + headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")";
                                        } else {
                                            result = headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")";
                                        }
                                        if (finalMismatchesByFile.containsKey(row.getKey())) {
                                            byHeader = finalMismatchesByFile.get(row.getKey());
                                            byHeader.put(headerId, "(Prims->" + number + " Echo->" + numberToCompare + ")");
                                        } else {
                                            byHeader = new HashMap<>();
                                            byHeader.put(headerId, "(Prims->" + number + " Echo->" + numberToCompare + ")");
                                        }

                                        finalMismatchesByFile.put(row.getKey(), byHeader);
                                        finalMismatches.put(row.getKey(), result);
                                        int counter = 1;
                                        if (counterMismatches.containsKey(headerId)) {
                                            counter = counterMismatches.get(headerId) + 1;
                                        }
                                        counterMismatches.put(headerId, counter);
                                    }
                                } else {
                                    try {
                                        number = new BigDecimal(value[i]).setScale(0, RoundingMode.HALF_UP);
                                        numberToCompare = new BigDecimal(valueToCompare[index]).setScale(0, RoundingMode.HALF_UP);
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                    if (number.subtract(numberToCompare).abs().compareTo(BigDecimal.ONE) > 0) {
                                        if (isMovement) {
                                            if (Arrays.asList("chg_eri_us").contains(headerId)) {
                                                BigDecimal curMthRateEcho = new BigDecimal(valueToCompare[findHeaderId("cur_mth_rte", headerToCompare)]);
                                                BigDecimal curEriLcPrism = new BigDecimal(value[findHeaderId("cur_eri_lc", headerToUse)]);
                                                BigDecimal prevMthRate = new BigDecimal(value[findHeaderId("prv_mth_rte", headerToUse)]);
                                                BigDecimal prvEriLc = new BigDecimal(value[findHeaderId("prv_eri_lc", headerToUse)]);

                                                BigDecimal newNumber = curMthRateEcho.multiply(curEriLcPrism).subtract(prevMthRate.multiply(prvEriLc));
                                                number = newNumber.setScale(0, RoundingMode.HALF_UP);
                                                numberToCompare = new BigDecimal(valueToCompare[index]).setScale(0, RoundingMode.HALF_UP);
                                                //Try compare with no exchangeRate impact
                                                if (newNumber.subtract(numberToCompare).abs().compareTo(BigDecimal.ONE) > 0) {
                                                    String result;
                                                    Map<String, String> byHeader;
                                                    if (finalMismatches.containsKey(row.getKey())) {
                                                        result = finalMismatches.get(row.getKey()) + " + " + headerId + "(Prims->" + number + " Echo->"
                                                                + numberToCompare + ")";
                                                    } else {
                                                        result = headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")";
                                                    }
                                                    if (finalMismatchesByFile.containsKey(row.getKey())) {
                                                        byHeader = finalMismatchesByFile.get(row.getKey());
                                                        byHeader.put(headerId, headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")");
                                                    } else {
                                                        byHeader = new HashMap<>();
                                                        byHeader.put(headerId, headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")");
                                                    }

                                                    finalMismatchesByFile.put(row.getKey(), byHeader);
                                                    finalMismatches.put(row.getKey(), result);
                                                    int counter = 1;
                                                    if (counterMismatches.containsKey(headerId)) {
                                                        counter = counterMismatches.get(headerId) + 1;
                                                    }
                                                    counterMismatches.put(headerId, counter);
                                                }
                                            } else if (Arrays.asList("cur_mth_rte", "cur_pl_rte", "prv_csh_rte", "cur_csh_rte", "prv_pl_rte")
                                                    .contains(headerId)) {
                                                try {
                                                    number = new BigDecimal(value[i]).setScale(3, RoundingMode.HALF_UP);
                                                    numberToCompare = new BigDecimal(valueToCompare[index]).setScale(3, RoundingMode.HALF_UP);
                                                } catch (Exception exception) {
                                                    exception.printStackTrace();
                                                }
                                                if (number.subtract(numberToCompare).abs().compareTo(new BigDecimal("0.001")) > 0) {
                                                    String result;
                                                    Map<String, String> byHeader;
                                                    if (finalMismatches.containsKey(row.getKey())) {
                                                        result = finalMismatches.get(row.getKey()) + " + " + headerId + "(Prims->" + number + " Echo->"
                                                                + numberToCompare + ")";
                                                    } else {
                                                        result = headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")";
                                                    }
                                                    if (finalMismatchesByFile.containsKey(row.getKey())) {
                                                        byHeader = finalMismatchesByFile.get(row.getKey());
                                                        byHeader.put(headerId, headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")");
                                                    } else {
                                                        byHeader = new HashMap<>();
                                                        byHeader.put(headerId, headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")");
                                                    }

                                                    finalMismatchesByFile.put(row.getKey(), byHeader);
                                                    finalMismatches.put(row.getKey(), result);
                                                    int counter = 1;
                                                    if (counterMismatches.containsKey(headerId)) {
                                                        counter = counterMismatches.get(headerId) + 1;
                                                    }
                                                    counterMismatches.put(headerId, counter);
                                                }
                                            }
                                        } else {
                                            String result;
                                            Map<String, String> byHeader;
                                            if (finalMismatches.containsKey(row.getKey())) {
                                                result =
                                                        finalMismatches.get(row.getKey()) + " + " + headerId + "(Prims->" + number + " Echo->" + numberToCompare
                                                                + ")";
                                            } else {
                                                result = headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")";
                                            }
                                            if (finalMismatchesByFile.containsKey(row.getKey())) {
                                                byHeader = finalMismatchesByFile.get(row.getKey());
                                                byHeader.put(headerId, headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")");
                                            } else {
                                                byHeader = new HashMap<>();
                                                byHeader.put(headerId, headerId + "(Prims->" + number + " Echo->" + numberToCompare + ")");
                                            }

                                            finalMismatchesByFile.put(row.getKey(), byHeader);
                                            finalMismatches.put(row.getKey(), result);
                                            int counter = 1;
                                            if (counterMismatches.containsKey(headerId)) {
                                                counter = counterMismatches.get(headerId) + 1;
                                            }
                                            counterMismatches.put(headerId, counter);
                                        }
                                    }
                                }
                            } else {
                                boolean skipIssue = false;
                                if (headerId.contains("sml_grps_pool_indic")) {
                                    if (value[i].equals("1") && valueToCompare[index].equals("2")) {
                                        //                                        skipIssue = true;
                                    }
                                }
                                if (!skipIssue) {
                                    String result;
                                    //                                    Map<String, String> byHeader;
                                    if (finalMismatches.containsKey(row.getKey())) {
                                        result =
                                                finalMismatches.get(row.getKey()) + " + " + headerId + "(Prims->" + value[i] + " Echo->" + valueToCompare[index]
                                                        + ")";
                                        //                                        result =
                                        //                                            finalMismatches.get(row.getKey()) + "UPDATE snpshot SET " + headerId + " = " + (valueToCompare[index].equals("") ?
                                        //                                                "null" :
                                        //                                                valueToCompare[index]) + " " + getWhereClause(headerToUse, valueToCompare);
                                    } else {
                                        result = headerId + "(Prims->" + value[i] + " Echo->" + valueToCompare[index] + ")";
                                        //                                        result =
                                        //                                            "UPDATE snpshot SET " + headerId + " = " + (valueToCompare[index].equals("") ? "null" : valueToCompare[index]) + " "
                                        //                                                + getWhereClause(headerToUse, valueToCompare);
                                    }
                                    //                                                                        if (finalMismatchesByFile.containsKey(row.getKey())) {
                                    //                                                                            byHeader = finalMismatchesByFile.get(row.getKey());
                                    //                                                                            byHeader.put(headerId, headerId + "(Prims->" + value[i] + " Echo->" + valueToCompare[index] + ")");
                                    //                                                                        } else {
                                    //                                                                            byHeader = new HashMap<>();
                                    //                                                                            byHeader.put(headerId, headerId + "(Prims->" + value[i] + " Echo->" + valueToCompare[index] + ")");
                                    //                                                                        }

                                    //                                                                        finalMismatchesByFile.put(row.getKey(), byHeader);
                                    finalMismatches.put(row.getKey(), result);
                                    int counter = 1;
                                    if (counterMismatches.containsKey(headerId)) {
                                        counter = counterMismatches.get(headerId) + 1;
                                    }
                                    counterMismatches.put(headerId, counter);
                                }
                            }
                        }
                    }
                }
            } else {
                //finalMismatches.put(row.getKey(), "DELETE FROM SNAPSHOT " + getWhereClause(headerToUse, value));
                Map<String, String> map = new HashMap<>();
                map.put("NOT_FOUND", "NOT_FOUND");
                finalMismatchesByFile.put(row.getKey(), map);
                finalMismatches.put(row.getKey(), "NOT_FOUND");
                int counter = 1;
                if (counterMismatches.containsKey("NOT_FOUND")) {
                    counter = counterMismatches.get("NOT_FOUND") + 1;
                }
                counterMismatches.put("NOT_FOUND", counter);
            }
        }
        String counterHeader = "COUNTER_HEADER: " + counterMismatches.entrySet().stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(x -> x.getKey() + "-> " + x.getValue() + " ").collect(Collectors.joining());
        //String counterRow =
        //        for (Map.Entry<String, Map<String, String>> entry : finalMismatchesByFile.entrySet()) {
        //            Map<String, String> value = entry.getValue();
        //            for (Map.Entry<String, String> x : value.entrySet()) {
        //                Map<String, String> valueToPrint = new HashMap<>();
        //                valueToPrint.put(entry.getKey(), x.getValue());
        //                exportToFileAppend(fileHandler, directory, valueToPrint, x.getKey());
        //            }
        //        }
        exportToFile(fileHandler, directory, finalMismatches, "finalMismatches", counterHeader);
    }

    private static int findHeaderId(String valueToFind, String[] headerToCompare) {
        int id = -1;
        for (int i = 0; i < headerToCompare.length; i++) {
            if (Arrays.asList("ctry_num", "mth_exch_rate", "re_prem_usd", "eri_usd").contains(valueToFind)) {
                id = -1;
                break;
            }
            if (headerToCompare[i].equals(valueToFind)) {
                id = i;
                break;
            }
        }
        return id;
    }

    private static void exportToFile(final FileHandler fileHandler, final JFileChooser file, Map<String, String> finalString, String fileName, String counter)
            throws IOException {
        String currentPath = file.getCurrentDirectory().getAbsolutePath();
        final PrintWriter pw = fileHandler.getFileWriter(currentPath + "\\" + fileName + ".txt");
        pw.println(counter);
        for (Map.Entry<String, String> str : finalString.entrySet()) {
            pw.println("ID: " + str.getKey() + " Value_Mismathces: " + str.getValue());
//            pw.println(str.getValue());
        }
        pw.close();
    }


    private static void exportToFileAppend(final FileHandler fileHandler, final JFileChooser file, Map<String, String> finalString, String fileName)
            throws IOException {
        String currentPath = file.getCurrentDirectory().getAbsolutePath();
        final PrintWriter pw = fileHandler.getFileWriterAppend(currentPath + "\\ByType\\" + fileName + ".txt");
        for (Map.Entry<String, String> str : finalString.entrySet()) {
            pw.println("ID: " + str.getKey() + " Value_Mismathces: " + str.getValue());
        }
        pw.close();
    }

    private static Map<String, String> loadFileAndMapIntoBean(final FileHandler fileHandler, final JFileChooser file) throws IOException {
        Map<String, String> mapBean = new HashMap<>();
        final BufferedReader br = fileHandler.getFileReader(file);
        String line;
        boolean isHeader = true;
        while ((line = br.readLine()) != null) {
            if (isHeader) {
                mapBean.put("HEADER", StringUtils.substringAfter(line, ";"));
                isHeader = false;
            } else {
                String id = StringUtils.substringBefore(line, ";");
                if (!id.equals("#N/D")) {
                    String value = StringUtils.substringAfter(line, ";");
                    mapBean.put(id, value);
                }
            }
        }
        br.close();
        return mapBean;
    }

    private static String getWhereClause(String[] headerToUse, String[] value) {
        int cntl_num = findHeaderId("cntl_num", headerToUse);
        int pcc_num = findHeaderId("pcc_num", headerToUse);
        int da_indic = findHeaderId("da_indic", headerToUse);
        int lob_id = findHeaderId("lob_id", headerToUse);
        int pye_date = findHeaderId("pye_date", headerToUse);
        int ri = findHeaderId("ri", headerToUse);
        int typ = findHeaderId("typ", headerToUse);
        int subg_id = findHeaderId("subg_id", headerToUse);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return "WHERE PERIOD = 321 AND cntl_num = '" + value[cntl_num] + "' AND pcc_num = '" + value[pcc_num] + "' AND da_indic = '" + value[da_indic]
                + "' AND lob_id = " + value[lob_id] + " AND pye_date = '" + LocalDate.parse(value[pye_date], formatter).toString() + "' AND ri = '" + value[ri]
                + "' AND typ = '" + value[typ] + "' AND subg_id = '" + value[subg_id] + "';";
    }
}
