package org.hupo.psi.mi.psicquic.view.webapp.io.download.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.hupo.psi.mi.psicquic.view.webapp.controller.BaseController;
import org.hupo.psi.mi.psicquic.view.webapp.controller.search.SearchController;
import org.hupo.psi.mi.psicquic.view.webapp.controller.search.UserQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 23/08/2012
 * Time: 10:36
 * To change this template use File | Settings | File Templates.
 */

@Controller("fileDownload")
@Scope("conversation.access")
@ConversationName("general")
public class FileDownloadController extends BaseController {

	private static final Log log = LogFactory.getLog(FileDownloadController.class);


	private List<SelectItem> selectItems;
	private List<SelectItem> selectClusteredItems;

    private static final int XML_INTERACTIONS_LIMIT = 500;

    @Autowired
    private UserQuery userQuery;

    @Autowired
    private SearchController searchController;


	private String selectedFormat;


	final private static List<String> formatsForSmallQueries = new ArrayList<String>(Arrays.asList(new String[]
			{"xml25", "biopax", "biopax-L2", "biopax-L3", "rdf-xml", "rdf-xml-abbrev", "rdf-n3", "rdf-turtle"}));

	final private static List<String> formatsForNegativeQueries = new ArrayList<String>(Arrays.asList(new String[]
			{"tab26", "tab27", "xml25"}));

	public FileDownloadController() {
		this.selectItems = new ArrayList<SelectItem>();
		this.selectClusteredItems = new ArrayList<SelectItem>();
    }

	void checkDownloads(){

		if(userQuery.isIncludeNegativeQuery()){
			String message = "Your results may contain negative interactions.";
			String detail = "They can only be exported in MITAB 2.6, MITAB 2.7 and XML 2.5.";
			addInfoMessage(message, detail);
		}

		if(isBigQuery()){
			String message = "Only up to "+ getXmlLimit() + " interactions can be downloaded in XML 2.5, BIOPAX and RFD formats.";
			String detail = " Apologies for the inconvenience.";
			addWarningMessage(message, detail);
		}
	}

	private void createList() {

		ResourceBundle rb = ResourceBundle.getBundle("org.hupo.psi.mi.psicquic.Messages");

		List<String> formats = searchController.getSelectedService().getFormats();

		if (formats != null && !formats.isEmpty()) {
			this.selectItems.clear();
			for (String format : formats) {
				boolean disable = false;
				if (isBigQuery() && formatsForSmallQueries.contains(format)) {
					disable = true;
				}
				if (userQuery.isOnlyNegativeQuery() && !(formatsForNegativeQueries.contains(format))) {
					disable = true;
				}
				this.selectItems.add(new SelectItem(format, rb.getString(format).trim(), rb.getString(format).trim(), disable));
			}
		}
	}

    private void createClusteredList() {
        ResourceBundle rb = ResourceBundle.getBundle("org.hupo.psi.mi.psicquic.Messages");
        String format = "tab25";
        this.selectClusteredItems.clear();
        this.selectClusteredItems.add(new SelectItem(format, rb.getString(format).trim()));
    }


	public List<SelectItem> getSelectItems() {
        createList();
        setSelectedFormat(null);
        checkDownloads();
        return selectItems;
	}

    public List<SelectItem> getSelectClusteredItems() {
        createClusteredList();
        setSelectedFormat(null);

        return selectClusteredItems;
    }

	public void setSelectItems(List<SelectItem> selectItems) {
		this.selectItems = selectItems;
	}

	public void setSelectClusteredItems(List<SelectItem> selectClusteredItems) {
		this.selectClusteredItems = selectClusteredItems;
	}

	public String getSelectedFormat() {
		return selectedFormat;
	}

	public void setSelectedFormat(String selectedFormat) {
		this.selectedFormat = selectedFormat;
	}

	public String cancelDialog() {

		if (RequestContext.getCurrentInstance() != null) {
			RequestContext.getCurrentInstance().returnFromDialog(null, null);
		}

		return null;
	}

	public String binaryDownload() {

		String download = "binary";

		if (RequestContext.getCurrentInstance() != null) {
			RequestContext.getCurrentInstance().returnFromDialog(download, null);
		}

		return null;
	}

	public String clusteredDownload() {

		String download = "clustered";

		if (RequestContext.getCurrentInstance() != null) {
			RequestContext.getCurrentInstance().returnFromDialog(download, null);
		}

		return null;
	}

	public void forward(ReturnEvent returnEvent) {

		if (returnEvent.getReturnValue() instanceof String) {
			String value = (String) returnEvent.getReturnValue();

			FacesContext context = FacesContext.getCurrentInstance();
			String baseURL = context.getExternalContext().getRequestContextPath();
			String serviceURL = searchController.getSelectedService().getRestUrl();
			String query = userQuery.getSearchQuery();

			if (value.contentEquals("binary")) {

				String url = baseURL + "/binaryDownload?" +
						"&serviceURL=" + serviceURL +
						"&query=" + query +
						"&format=" + getSelectedFormat();

				try {
					String encodeURL = context.getExternalContext().encodeResourceURL(url);
					context.getExternalContext().redirect(encodeURL);
				} catch (Exception e) {
					final String msg = "The content of the file can not be generated";
					final String detail = "The " + url + "for download the file in not available";
					addErrorMessage(msg, detail);
					log.error("The content of the file can not be generated: " + url, e);

				} finally {
					context.responseComplete();
				}
			} else if (value.contentEquals("clustered")) {

				String format = getSelectedFormat();

				if (format != null) {
					format = "psi-mi/" + format;
				}

				String url = baseURL + serviceURL +
						"&format=" + format;

				try {

					String encodeURL = context.getExternalContext().encodeResourceURL(url);
					context.getExternalContext().redirect(encodeURL);
				} catch (Exception e) {
					final String msg = "The content of the file can not be generated";
					final String detail = "The " + url + "for download the file in not available";
					addErrorMessage(msg, detail);
					log.error("The content of the file can not be generated: " + url, e);
				} finally {
					context.responseComplete();
				}
			}
		}
	}

	public void selectedFormatChanged(ValueChangeEvent valueChangeEvent) {
		if (valueChangeEvent.getNewValue() instanceof String) {
			selectedFormat = ((String) valueChangeEvent.getNewValue());
		}
	}

    public boolean isBigQuery() {
        return searchController.getSelectedService().getHits() > XML_INTERACTIONS_LIMIT;
    }

    public Integer getXmlLimit() {
        return XML_INTERACTIONS_LIMIT;
    }

}
