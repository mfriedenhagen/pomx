package com.github.t1.pomx;

import com.github.t1.xml.*;
import com.github.t1.xml.XmlElement;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.List;

import static com.github.t1.xml.XmlElement.*;

@RequiredArgsConstructor
class ProjectObjectModel {
    static ProjectObjectModel from(String xml) { return new ProjectObjectModel(Xml.fromString(xml)); }

    static ProjectObjectModel readFrom(Path path) { return new ProjectObjectModel(Xml.load(path.toUri())); }

    private final Xml in;
    private Xml out;

    String asString() {
        return converted().toXmlString();
    }

    private Xml converted() {
        if (out == null) {
            out = Xml.fromString(in.toXmlString());
            expand();
        }
        return out;
    }

    private void expand() {
        expandModelVersion();
        expandGav();
        expandDependencyManagement();
    }

    private void expandModelVersion() {
        out.addElement("modelVersion", atBegin()).addText("4.0.0");
    }

    private void expandGav() {
        List<XmlElement> packagings = out.find("/project/*[local-name()='war' or local-name()='jar']");
        if (packagings.isEmpty())
            return;
        if (packagings.size() > 1)
            throw new RuntimeException("multiple packagings found");
        XmlElement packaging = packagings.get(0);
        GAV gav = GAV.split(packaging.getText());
        XmlElement project = packaging.getParent();
        project.addElement("groupId", before(packaging)).addText(gav.getGroupId());
        project.addElement("artifactId", before(packaging)).addText(gav.getArtifactId());
        project.addElement("version", before(packaging)).addText(gav.getVersion());
        if (gav.getClassifier() != null)
            project.addElement("classifier", before(packaging)).addText(gav.getClassifier());
        project.addElement("packaging", before(packaging)).addText(packaging.getName());
        packaging.remove();
    }

    private void expandDependencyManagement() {
        out.find("/project/dependencyManagement/dependencies")
           .forEach(dependencies -> dependencies
                   .find("pom")
                   .forEach(dependency -> {
                       GAV gav = GAV.split(dependency.getText());
                       XmlElement element = dependencies.addElement("dependency", before(dependency));
                       element.addElement("groupId").addText(gav.getGroupId());
                       element.addElement("artifactId").addText(gav.getArtifactId());
                       element.addElement("version").addText(gav.getVersion());
                       element.addElement("scope").addText("import");
                       element.addElement("type").addText(dependency.getName());
                       dependency.remove();
                   }));
    }
}