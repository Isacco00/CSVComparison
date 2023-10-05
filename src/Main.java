import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        try {
            DBConnection dbConnection = new DBConnection();
            final FileHandler fileHandler = new FileHandler();
            final JFileChooser configFile = fileHandler.openPopup();
            TasksBean wrapper = loadFile(fileHandler, configFile);
            List<Map<String, String>> connectionsParameters = wrapper.getComparisonDatabases();
            List<Connection> connections = new ArrayList<>();
            List<Map<String, String>> listComparisonBean = new ArrayList<>();
            for (Map<String, String> params : connectionsParameters) {
                connections.add(dbConnection.connect(params.get("DBUrl"), params.get("DBName"), params.get("DBPassword")));
            }
            for (Connection connection : connections) {
                List<String> columnNames = new ArrayList<>();
                Statement stmt = connection.createStatement();
                for (String query : wrapper.getQueries()) {
                    ResultSet rs = stmt.executeQuery(query);
                    if (rs != null) {
                        Map<String, String> bean = new HashMap<>();
                        bean.put("HEADER", "");
                        ResultSetMetaData columns = rs.getMetaData();
                        int i = 0;
                        while (i < columns.getColumnCount()) {
                            i++;
                            bean.put("HEADER", bean.get("HEADER") + columns.getColumnName(i) + ";");
                            columnNames.add(columns.getColumnName(i));
                        }
                        int rowNumber = 0;
                        while (rs.next()) {
                            StringBuilder rowValues = new StringBuilder();
                            for (i = 0; i < columnNames.size(); i++) {
                                rowValues.append(rs.getString(columnNames.get(i))).append(";");
                            }
                            bean.put(Integer.toString(rowNumber), rowValues.toString());
                            rowNumber++;
                        }
                        listComparisonBean.add(bean);
                    }
                }
            }
            analyzeAndExportBean(wrapper.getSkipHeader(), listComparisonBean, configFile, fileHandler);
            JOptionPane.showMessageDialog(new JFrame(), "Analisi conclusa correttamente.", "Dialog", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(new JFrame(), "Errore interno", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    private static void analyzeAndExportBean(List<String> skipHeader, List<Map<String, String>> comparisonBean, JFileChooser directory, FileHandler fileHandler) throws IOException {
        Iterator<Map<String, String>> it = comparisonBean.iterator();
        Map<String, String> bean = it.next();
        boolean append = false;
        while (it.hasNext()) {
            Map<String, String> beanToCompare = it.next();
            Map<String, String> finalMismatches = new HashMap<>();
            Map<String, Map<String, String>> finalMismatchesByFile = new HashMap<>();
            Map<String, Integer> counterMismatches = new HashMap<>();
            String[] headerToUse = bean.get("HEADER").split(";");
            String[] headerToCompare = beanToCompare.get("HEADER").split(";");
            bean.remove("HEADER");
            beanToCompare.remove("HEADER");
            for (int i = 0; i < headerToUse.length; i++) {
                boolean doCompare = true;
                String headerId = headerToUse[i];
                int index = findHeaderId(headerId, headerToCompare);
                if (index != -1) {//If header not find skip value
                    if (skipHeader.contains(headerId)) {
                        doCompare = false;
                    }
                    if (doCompare) {
                        for (Map.Entry<String, String> row : bean.entrySet()) {
                            if (bean.get(row.getKey()) != null) {
                                String value = row.getValue().split(";")[i];
                                String valueToCompare = beanToCompare.get(row.getKey()).split(";")[i];
                                if (!value.equals(valueToCompare)) {
                                    String result;
                                    if (finalMismatches.containsKey(row.getKey())) {
                                        result = finalMismatches.get(row.getKey()) + " + " + headerId + "(Prims->" + value + " Echo->" + valueToCompare + ")";
                                    } else {
                                        result = headerId + "(Prims->" + value + " Echo->" + valueToCompare + ")";
                                    }
                                    finalMismatches.put(row.getKey(), result);
                                    int counter = 1;
                                    if (counterMismatches.containsKey(headerId)) {
                                        counter = counterMismatches.get(headerId) + 1;
                                    }
                                    counterMismatches.put(headerId, counter);
                                }
                            } else {
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
                    }
                }
            }
            String counterHeader = "COUNTER_HEADER: " + counterMismatches.entrySet().stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).map(x -> x.getKey() + "-> " + x.getValue() + " ").collect(Collectors.joining());
//                    for (Map.Entry<String, Map<String, String>> entry : finalMismatchesByFile.entrySet()) {
//                        Map<String, String> value = entry.getValue();
//                        for (Map.Entry<String, String> x : value.entrySet()) {
//                            Map<String, String> valueToPrint = new HashMap<>();
//                            valueToPrint.put(entry.getKey(), x.getValue());
//                            exportToFileAppend(fileHandler, directory, valueToPrint, x.getKey());
//                        }
//                    }
            exportToFile(fileHandler, directory, finalMismatches, "finalMismatches", counterHeader, append);
            append = true;
        }
    }

    private static int findHeaderId(String valueToFind, String[] headerToCompare) {
        int id = -1;
        for (int i = 0; i < headerToCompare.length; i++) {
            if (headerToCompare[i].equals(valueToFind)) {
                id = i;
                break;
            }
        }
        return id;
    }

    private static void exportToFile(final FileHandler fileHandler, final JFileChooser file, Map<String, String> finalString, String fileName,
                                     String counter, boolean appendMode) throws IOException {
        String currentPath = file.getCurrentDirectory().getAbsolutePath();
        final PrintWriter pw;
        if (appendMode) {
            pw = fileHandler.getFileWriterAppend(currentPath + "\\" + fileName + ".txt");
        } else {
            pw = fileHandler.getFileWriter(currentPath + "\\" + fileName + ".txt");
        }
        pw.println(counter);
        for (Map.Entry<String, String> str : finalString.entrySet()) {
            pw.println("ID: " + str.getKey() + " Value_Mismathces: " + str.getValue());
        }
        pw.close();
    }


    private static void exportToFileAppend(final FileHandler fileHandler, final JFileChooser file, Map<String, String> finalString, String fileName) throws IOException {
        String currentPath = file.getCurrentDirectory().getAbsolutePath();
        final PrintWriter pw = fileHandler.getFileWriterAppend(currentPath + "\\ByType\\" + fileName + ".txt");
        for (Map.Entry<String, String> str : finalString.entrySet()) {
            pw.println("ID: " + str.getKey() + " Value_Mismathces: " + str.getValue());
        }
        pw.close();
    }

    private static TasksBean loadFile(final FileHandler fileHandler, final JFileChooser file) throws Exception {
        List<Map<String, String>> listConnections = new ArrayList<>();
        final BufferedReader br = fileHandler.getFileReader(file);
        String line;
        List<String> queries = new ArrayList<>();
        List<String> skipHeader = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if (line.contains("DBUrl")) {
                Map<String, String> connectionParameter = new HashMap<>();
                if (!StringUtils.substringAfter(line, "DBUrl:").equals("")) {
                    connectionParameter.put("DBUrl", StringUtils.substringAfter(line, "DBUrl:"));
                } else {
                    throw new Exception("Unexpected");
                }
                line = br.readLine();
                if (line != null && !StringUtils.substringAfter(line, "DBName:").equals("")) {
                    connectionParameter.put("DBName", StringUtils.substringAfter(line, "DBName:"));
                } else {
                    throw new Exception("Unexpected");
                }
                line = br.readLine();
                if (line != null && !StringUtils.substringAfter(line, "DBPassword:").equals("")) {
                    connectionParameter.put("DBPassword", StringUtils.substringAfter(line, "DBPassword:"));
                } else {
                    throw new Exception("Unexpected");
                }
                listConnections.add(connectionParameter);
            } else if (line.contains("query:")) {
                if (!StringUtils.substringAfter(line, "query:").equals("")) {
                    queries.add(StringUtils.substringAfter(line, "query:"));
                }
            } else if (line.contains("skipHeader:")) {
                if (!StringUtils.substringAfter(line, "skipHeader:").equals("")) {
                    skipHeader.add(StringUtils.substringAfter(line, "skipHeader:"));
                }
            }
        }
        br.close();
        TasksBean tasksBean = new TasksBean();
        tasksBean.setComparisonDatabases(listConnections);
        tasksBean.setQueries(queries);
        tasksBean.setSkipHeader(skipHeader);
        return tasksBean;
    }

}
