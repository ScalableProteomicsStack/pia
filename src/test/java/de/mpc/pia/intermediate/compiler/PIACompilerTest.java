package de.mpc.pia.intermediate.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.peptide.ReportPeptide;
import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;

import static org.junit.Assert.*;

public class PIACompilerTest {

    private File mzid55mergeTandem;
    private File mzid55mergeMascot;
    private File mzid55mergeOmssa;

    private File idXMLtandemFile;
    private File idXMLmsgfFile;
    private File idXMLexpectedFile;
    private double scoreDelta = 0.000001;

    private String piaIntermediateFileName = "PIACompilerTest.pia.xml";


    @Before
    public void setUp() {

        mzid55mergeTandem = new File(PIACompilerTest.class.getResource("/55merge_tandem.mzid").getPath());
        mzid55mergeMascot = new File(PIACompilerTest.class.getResource("/55merge_mascot_full.mzid").getPath());
        mzid55mergeOmssa = new File(PIACompilerTest.class.getResource("/55merge_omssa.mzid").getPath());

        idXMLtandemFile = new File(PIACompilerTest.class.getResource("/merge1-tandem-fdr_filtered-015.idXML").getPath());
        idXMLmsgfFile = new File(PIACompilerTest.class.getResource("/merge1-msgf-fdr_filtered-015.idXML").getPath());

        idXMLexpectedFile = new File(PIACompilerTest.class.getResource("/yeast-gold-015-filtered-proteins.csv").getPath());
    }


    @Test
    public void testPIACompilerCompilationAndAnalysis() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertTrue(piaCompiler.getDataFromFile("tandem", idXMLtandemFile.getAbsolutePath(), null, null));
        assertTrue(piaCompiler.getDataFromFile("msgf", idXMLmsgfFile.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");


        // write out the file
        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaCompiler.finish();

        // now make an analysis
        PIAModeller piaModeller = new PIAModeller(piaIntermediateFile.getAbsolutePath());

        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("s.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.setConsiderModifications(false);

        /*
         Validation of the filter with PSMs counts by mzIdentFILE
         */

        List<AbstractFilter> filters = new ArrayList<>();
        filters.add(new PSMScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));

        List<ReportPeptide> peptides = piaModeller.getPeptideModeller().getFilteredReportPeptides(0L, filters);
        assertEquals("Wrong number of peptides in the returned list", 2611,  peptides.size());

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        piaModeller.getProteinModeller().updateFDRData(DecoyStrategy.ACCESSIONPATTERN, "s.*", 0.01);
        piaModeller.getProteinModeller().updateDecoyStates();
        piaModeller.getProteinModeller().calculateFDR();


        filters = new ArrayList<>();
        filters.add(RegisteredFilters.NR_GROUP_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER.newInstanceOf(
                        FilterComparator.greater_equal, 2, false));



        // get the expected values
        Map<String, List<Object>> expectedValues = new HashMap<>(426);

        BufferedReader br = new BufferedReader(new FileReader(idXMLexpectedFile));
        String line;
        int nrLine = 0;


        while ((line = br.readLine()) != null) {
            if (nrLine++ < 1) {
                continue;
            }

            String split[] = line.split("\",\"");
            List<Object> entries = new ArrayList<>();

            entries.add(Double.parseDouble(split[1]));
            entries.add(Integer.parseInt(split[2]));
            entries.add(Integer.parseInt(split[3]));
            entries.add(Integer.parseInt(split[4]));
            entries.add(Boolean.parseBoolean(split[5]));
            entries.add(Double.parseDouble(split[6]));
            entries.add(Double.parseDouble(split[7].substring(0, split[7].length()-1)));

            expectedValues.put(split[0].substring(1), entries);
        }
        br.close();

        // compare the values
        for (ReportProtein prot : piaModeller.getProteinModeller().getFilteredReportProteins(filters)) {
            StringBuilder accSb = new StringBuilder();
            for (Accession acc : prot.getAccessions()) {
                accSb.append(acc.getAccession());
                accSb.append(',');
            }
            accSb.deleteCharAt(accSb.length()-1);

            List<Object> values = expectedValues.get(accSb.toString());
            assertNotNull("These accessions are not expected: " + accSb, values);

            assertEquals("Wrong score for " + accSb, (Double)(values.get(0)), prot.getScore(), scoreDelta);
            assertEquals("Wrong number of peptides for " + accSb, values.get(1), prot.getNrPeptides());
            assertEquals("Wrong number of PSMs for " + accSb, values.get(2), prot.getNrPSMs());
            assertEquals("Wrong number of spectra for " + accSb, values.get(3), prot.getNrSpectra());
            assertEquals("Wrong decoy state for " + accSb, values.get(4), prot.getIsDecoy());
            assertEquals("Wrong FDR for " + accSb, (Double)(values.get(5)), prot.getFDR(), scoreDelta);
            assertEquals("Wrong q-value for " + accSb, (Double)(values.get(6)), prot.getQValue(), scoreDelta);
        }

        piaIntermediateFile.delete();
    }


    @Test
    public void testPIACompilerMzidFiles() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertTrue("X!TAndem file could not be parsed", piaCompiler.getDataFromFile("tandem", mzid55mergeTandem.getAbsolutePath(), null, null));

        assertTrue("OMSSA file could not be parsed", piaCompiler.getDataFromFile("tandem", mzid55mergeOmssa.getAbsolutePath(), null, null));

        assertTrue("Mascot file could not be parsed", piaCompiler.getDataFromFile("mascot", mzid55mergeMascot.getAbsolutePath(), null, null));


        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        // test writing using the file
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaIntermediateFile.delete();

        // test writing using the file's name
        piaCompiler.writeOutXML(piaIntermediateFile.getAbsolutePath());
        piaIntermediateFile.delete();

        piaCompiler.finish();
    }


    @Test
    public void testPIACompilerWriteXML() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertTrue("X!TAndem file could not be parsed", piaCompiler.getDataFromFile("tandem", mzid55mergeTandem.getAbsolutePath(), null, null));

        assertTrue("Mascot file could not be parsed", piaCompiler.getDataFromFile("mascot", mzid55mergeMascot.getAbsolutePath(), null, null));


        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        // test writing using the file's name
        piaCompiler.writeOutXML(piaIntermediateFile.getAbsolutePath());
        piaIntermediateFile.delete();

        // test writing using the file object
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaIntermediateFile.delete();

        // test writing using the file stream
        try (FileOutputStream fos = new FileOutputStream(piaIntermediateFile)) {
            piaCompiler.writeOutXML(fos);
            piaIntermediateFile.delete();
        }

        piaCompiler.finish();
    }
}
