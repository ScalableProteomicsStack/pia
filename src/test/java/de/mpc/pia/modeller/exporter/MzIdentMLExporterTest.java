package de.mpc.pia.modeller.exporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;


public class MzIdentMLExporterTest {

    public static File piaFile;

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MzIdentMLExporterTest.class);


    @Before
    public void setUp() throws Exception {
        piaFile = new File(MzTabExporterTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
        //piaFile = new File("/mnt/data/uniNOBACKUP/PIA/PRD000397/PRD000397.pia.xml");
    }


    @Test
    public void testMzIdentMLExporter() throws JAXBException, XMLStreamException, IOException {
        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();


        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));


        seInference.setScoring(new MultiplicativeScoring(new HashMap<String, String>()));
        seInference.getScoring().setSetting(AbstractScoring.scoringSettingID, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.scoringSpectraSettingID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);


        // simple exporting
        MzIdentMLExporter exporter = new MzIdentMLExporter(piaModeller);
        File exportFile = File.createTempFile("pia_testMzIdentML", ".mzIdentML");

        assertTrue(exporter.exportToMzIdentML(0L, exportFile, true, false));
    }
}
