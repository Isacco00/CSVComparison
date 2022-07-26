import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            final FileHandler fileHandler = new FileHandler();
            final JFileChooser file = fileHandler.openPopup();
            Map<String, String> stepBean = loadFileAndMapIntoBean(fileHandler, file);
            analyzeAndExportBean(stepBean, file, fileHandler);
            JOptionPane.showMessageDialog(new JFrame(),
                "Analisi conclusa correttamente.\n"
                    + "Troverai 3 file nella stessa cartella del file step che hai selezionato.\n"
                    + "Uno con lo stesso nome del file step rinominato in .csv e gli altri 2 con i punti dei 2 piani (PuntiPianoA, PuntiPianoB)",
                "Dialog", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(new JFrame(), "Errore generico", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private static void analyzeAndExportBean(Map<String, String> stepBean, JFileChooser directory, FileHandler fileHandler) throws IOException {
        List<String> advancedFaceFound = new ArrayList<>();
        String closedShellKey = "";
        for (Map.Entry<String, String> row : stepBean.entrySet()) {
            if (row.getValue().contains("CLOSED_SHELL")) {
                closedShellKey = row.getKey();
                break;
            }
        }
        String[] advancedFaceIdList = stepBean.get(closedShellKey).replaceAll("(([A-Z_();',]))", "").split("#");
        int max = 1, previousMax = 1;
        List<String> finalAdvancedFaceId = new ArrayList<>();
        for (int i = 1; i < advancedFaceIdList.length; i++) {
            String id = advancedFaceIdList[i];
            String advancedFace = stepBean.get(id);
            advancedFaceFound.add("#" + id + "= " + advancedFace);
            String[] boundsId = StringUtils.substringBefore(advancedFace.replaceAll("(([A-Z(_;',.]))", ""), ")").split("#");
            if (boundsId.length >= max) {
                max = boundsId.length;
                if (previousMax != max) {
                    finalAdvancedFaceId.clear();
                    previousMax = max;
                }
                finalAdvancedFaceId.add(id);
            }
        }
        //        /exportToCsv(fileHandler, directory, advancedFaceFound, "AdvancedFace");
        //CheckConsistency
        if (finalAdvancedFaceId.size() > 2) {
            throw new RuntimeException();
        }
        List<String> faceGeometryFound = new ArrayList<>();
        int counter = 0;
        List<String> pointPlaneA = new ArrayList<>();
        List<String> pointPlaneB = new ArrayList<>();
        for (String advancedFaceId : finalAdvancedFaceId) {
            String advancedFace = stepBean.get(advancedFaceId);
            String faceGeometryId =
                StringUtils.substringAfter(advancedFace.replaceAll("([A-Z(#_;',.])", ""), ")").replaceAll("([)])", ""); //Should always be PLANE
            String faceGeometry = stepBean.get(faceGeometryId);
            faceGeometryFound.add("#" + advancedFaceId + "= " + advancedFace);
            faceGeometryFound.add("#" + faceGeometryId + "= " + faceGeometry);
            faceGeometryFound.add("");
            String[] boundsId = StringUtils.substringBefore(advancedFace.replaceAll("([A-Z(_;',.])", ""), ")").split("#");
            for (int i = 1; i < boundsId.length; i++) {
                String faceId = boundsId[i];
                String face = stepBean.get(faceId);
                faceGeometryFound.add("#" + faceId + "= " + face);
            }
            faceGeometryFound.add("");
            List<String> orientedEdgeIdList = new ArrayList<>();
            for (int i = 1; i < boundsId.length; i++) {
                String faceId = boundsId[i];
                String edgeLoopId = stepBean.get(faceId).replaceAll("([A-Z#()_;',.])", "");
                String edgeLoop = stepBean.get(edgeLoopId);
                String[] orientedEdgeId = edgeLoop.replaceAll("([A-Z()_;',.])", "").split("#");
                for (int j = 1; j < orientedEdgeId.length; j++) {
                    orientedEdgeIdList.add(orientedEdgeId[j]);
                }
                faceGeometryFound.add("#" + edgeLoopId + "= " + edgeLoop);
            }
            faceGeometryFound.add("");
            List<String> edgeCurveIdList = new ArrayList<>();
            for (String id : orientedEdgeIdList) {
                String orientedEdge = stepBean.get(id);
                String edgeCurveId = orientedEdge.replaceAll("(([A-Z()_;',.*#]))", "");
                edgeCurveIdList.add(edgeCurveId);
                faceGeometryFound.add("#" + id + "= " + orientedEdge);
            }
            faceGeometryFound.add("");
            List<String> edgeGeometryIdList = new ArrayList<>();
            for (String id : edgeCurveIdList) {
                String edgeCurve = stepBean.get(id);
                String[] edgeGeometryId = edgeCurve.replaceAll("([A-Z()_;',.*])", "").split("#");
                for (int j = 1; j < edgeGeometryId.length; j++) {
                    edgeGeometryIdList.add(edgeGeometryId[j]);
                }
                faceGeometryFound.add("#" + id + "= " + edgeCurve);
            }
            faceGeometryFound.add("");
            double perimeter = 0.0;
            StringBuilder perimeterValue = new StringBuilder();
            for (int i = 0; i < edgeGeometryIdList.size(); i++) {
                String id = edgeGeometryIdList.get(i);
                String edgeGeometry = stepBean.get(id);
                faceGeometryFound.add("#" + id + "= " + edgeGeometry);
                if (edgeGeometry.contains("LINE")) {
                    double value = getCartesianPoint(stepBean, faceGeometryFound, edgeGeometryIdList, i, counter, pointPlaneA, pointPlaneB);
                    perimeter = perimeter + value;
                    perimeterValue.append("+").append(" from line ").append(value);
                }
                if (edgeGeometry.contains("CIRCLE")) {
                    double distance = getCartesianPoint(stepBean, faceGeometryFound, edgeGeometryIdList, i, counter, pointPlaneA, pointPlaneB);
                    double radius = Double.parseDouble(edgeGeometry.replaceAll("([A-Z])+\\(|([#;)])", "").split(",")[2]);
                    double value;
                    if (distance == 0) {
                        value = radius * 2 * Math.PI;
                    } else {
                        value = radius * 2 * Math.asin(distance / (2 * radius));
                    }
                    perimeter = perimeter + value;
                    perimeterValue.append("+").append(" from circle ").append(value);
                }
            }
            faceGeometryFound.add("");
            faceGeometryFound.add("---------------------------------------------------------------------");
            faceGeometryFound.add("PERIMETRO FACCIA : " + perimeter);
            faceGeometryFound.add("PERIMETRO VALORI : " + perimeterValue);
            faceGeometryFound.add("---------------------------------------------------------------------");
            faceGeometryFound.add("");
            counter++;
        }
        exportToCsv(fileHandler, directory, faceGeometryFound,
            directory.getSelectedFile().getName().substring(0, directory.getSelectedFile().getName().length() - 4));
        exportToCsv(fileHandler, directory, pointPlaneA, "PuntiPianoA");
        exportToCsv(fileHandler, directory, pointPlaneB, "PuntiPianoB");

    }

    private static double getCartesianPoint(Map<String, String> stepBean, List<String> faceGeometryFound, List<String> edgeGeometryIdList, int i, int counter,
        List<String> pointPlaneA, List<String> pointPlaneB) {
        String cartesianPoint1Id = stepBean.get(edgeGeometryIdList.get(i - 2).replaceAll("([A-Z_()',#;])", "")).replaceAll("([A-Z_()',#;])", "");
        String cartesianPoint2Id = stepBean.get(edgeGeometryIdList.get(i - 1).replaceAll("([A-Z_()',#;])", "")).replaceAll("([A-Z_()',#;])", "");
        String cartesianPoint1 = stepBean.get(cartesianPoint1Id);
        String cartesianPoint2 = stepBean.get(cartesianPoint2Id);
        faceGeometryFound.add("#" + cartesianPoint1Id + "= " + cartesianPoint1);
        faceGeometryFound.add("#" + cartesianPoint2Id + "= " + cartesianPoint2);

        String[] cartesianPoint1Values = stepBean.get(cartesianPoint1Id).replaceAll("([()'#;])", "").split(",");
        String[] cartesianPoint2Values = stepBean.get(cartesianPoint2Id).replaceAll("([()'#;])", "").split(",");

        if (counter == 0) {
            pointPlaneA.add(cartesianPoint1Values[1] + " " + cartesianPoint1Values[2] + " " + cartesianPoint1Values[3]);
            pointPlaneA.add(cartesianPoint2Values[1] + " " + cartesianPoint2Values[2] + " " + cartesianPoint2Values[3]);
        } else {
            pointPlaneB.add(cartesianPoint1Values[1] + " " + cartesianPoint1Values[2] + " " + cartesianPoint1Values[3]);
            pointPlaneB.add(cartesianPoint2Values[1] + " " + cartesianPoint2Values[2] + " " + cartesianPoint2Values[3]);
        }
        return calcDistanceBetweenPoints(cartesianPoint1Values, cartesianPoint2Values);
    }

    private static double calcDistanceBetweenPoints(String[] cartesianPoint1Values, String[] cartesianPoint2Values) {
        double v = Math.pow((Double.parseDouble(cartesianPoint2Values[1]) - Double.parseDouble(cartesianPoint1Values[1])), 2) + Math.pow(
            (Double.parseDouble(cartesianPoint2Values[2]) - Double.parseDouble(cartesianPoint1Values[2])), 2) + Math.pow(
            (Double.parseDouble(cartesianPoint2Values[3]) - Double.parseDouble(cartesianPoint1Values[3])), 2);
        return Math.sqrt(v);
    }

    private static void exportToCsv(final FileHandler fileHandler, final JFileChooser file, List<String> finalString, String fileName) throws IOException {
        String currentPath = file.getCurrentDirectory().getAbsolutePath();
        final PrintWriter pw = fileHandler.getFileWriter(currentPath + "\\" + fileName + ".csv");
        for (String str : finalString) {
            pw.println(str);
        }
        pw.close();
    }

    private static Map<String, String> loadFileAndMapIntoBean(final FileHandler fileHandler, final JFileChooser file) throws IOException {
        Map<String, String> mapBean = new HashMap<>();
        final BufferedReader br = fileHandler.getFileReader(file);
        String line;
        boolean findContent = false, endContent = false;
        String lastID = "0";
        while ((line = br.readLine()) != null && !endContent) {
            if (findContent) {
                if (line.equals("ENDSEC;")) {
                    //Finish content
                    endContent = true;
                }
                if (line.charAt(0) == '#') {
                    if (line.contains("=")) {
                        String id = line.substring(1, line.indexOf("="));
                        mapBean.put(id, line.substring(line.indexOf("=") + 1));
                        lastID = id;
                    } else {
                        String valueToConcat = mapBean.get(lastID);
                        valueToConcat = valueToConcat.concat(line);
                        mapBean.replace(lastID, valueToConcat);
                    }
                }
            }
            if (line.equals("DATA;")) {
                //Start mapping into bean
                findContent = true;
            }
        }
        br.close();
        return mapBean;
    }
}
