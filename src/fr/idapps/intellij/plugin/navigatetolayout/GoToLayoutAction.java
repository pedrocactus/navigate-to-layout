package fr.idapps.intellij.plugin.navigatetolayout;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by castex on 23/02/2016.
 */
public class GoToLayoutAction extends AnAction {

    // If you register the action from Java code, this constructor is used to set the menu item name
    // (optionally, you can specify the menu description and an icon to display next to the menu item).
    // You can omit this constructor when registering the action in the plugin.xml file.
    public GoToLayoutAction() {
        // Set the menu item name.
        super("GoToLayoutAction");
        // Set the menu item name, description and icon.
        // super("Text _Boxes","Item description",IconLoader.getIcon("/Mypackage/icon.png"));
    }


    public void actionPerformed(AnActionEvent event) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());

        final Project project = event.getData(PlatformDataKeys.PROJECT);

        String rootPath = project.getBasePath() + "/app/src/main/res";

        List<VirtualFile> virtualFiles;

        String layoutName = null;
        try {
            for (String line : Files.readAllLines(Paths.get(file.getCanonicalPath(), ""), StandardCharsets.UTF_8)) {
                if (line.contains("R.layout")) {
                    String[] array = line.split("R.layout.");
                    layoutName = array[array.length - 1].split("\\)")[0];
                    layoutName = layoutName.split(";")[0];
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (layoutName != null && !layoutName.isEmpty()) {
            File[] domains = new File(rootPath).listFiles();

            virtualFiles = searchXmlLayoutFiles(layoutName, domains);

            if (!virtualFiles.isEmpty() && virtualFiles.size() > 1) {
                BaseListPopupStep<VirtualFile> baseListPopupStep = new BaseListPopupStep<VirtualFile>("Layout files are available", virtualFiles) {
                    @Override
                    public PopupStep onChosen(VirtualFile selectedValue, boolean finalChoice) {
                        new OpenFileDescriptor(project, selectedValue).navigateInEditor(project, false);
                        return super.onChosen(selectedValue, finalChoice);
                    }

                    @NotNull
                    @Override
                    public String getTextFor(VirtualFile value) {
                        return value.getCanonicalPath();
                    }
                };
                ListPopup listPopup = JBPopupFactory.getInstance().createListPopup(baseListPopupStep);
                listPopup.showInBestPositionFor(CommonDataKeys.EDITOR.getData(event.getDataContext()));
            } else if (!virtualFiles.isEmpty() && virtualFiles.size() == 1) {
                new OpenFileDescriptor(project, virtualFiles.get(0)).navigateInEditor(project, false);
            }
        }
    }

    private List<VirtualFile> searchXmlLayoutFiles(String fileName, File[] files) {
        List<VirtualFile> resultFiles = new ArrayList<VirtualFile>();
        for (File file : files) {
            if (!file.isDirectory()) {
                if (file.isFile() && file.getName().contains(fileName) && file.getAbsolutePath().endsWith(".xml") && file.getParentFile().isDirectory() && file.getParentFile().getName().startsWith("layout")) {
                    resultFiles.add(LocalFileSystem.getInstance().findFileByIoFile(new File(file.getAbsolutePath())));
                }
            } else {
                resultFiles.addAll(searchXmlLayoutFiles(fileName, new File(file.getAbsolutePath()).listFiles()));
            }
        }
        return resultFiles;
    }
}
