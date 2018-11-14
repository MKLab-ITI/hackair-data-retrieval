package gr.iti.mklab.misc;

import java.io.IOException;
import java.util.ArrayList;

import dcnn.DcnnPipeline;
import dcnn.classes.ConceptData;
import dcnn.classes.ImageConceptData;

public class DCNNExtractionTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArrayList<String> imgPathsList = new ArrayList<String>();

		String path = "C:/Users/Public/Pictures/Sample Pictures/";
		imgPathsList.add(path + "Chrysanthemum.jpg");
		imgPathsList.add(path + "Desert.jpg");

		String dcnnPath = "C:/Users/espyromi/Desktop/flickr_crawler_workspace/dcnn/exe/dcnn";

		try {

			DcnnPipeline dcp = new DcnnPipeline();
			ArrayList<ImageConceptData> imgCon = (ArrayList<ImageConceptData>) dcp.runPipeline(dcnnPath,
					imgPathsList, imgPathsList);

			for (int i = 0; i < imgCon.size(); i++) {
				String imageName = imgCon.get(i).getImageName();
				System.out.println(imageName);
				ArrayList<ConceptData> conData = (ArrayList<ConceptData>) imgCon.get(i).getConceptData();
				for (int j = 0; j < conData.size(); j++) {
					System.out.println(
							"\t" + conData.get(j).getConceptName() + " " + conData.get(j).getConceptValue());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}

	}
}
