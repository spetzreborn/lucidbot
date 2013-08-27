package web.models;

import database.models.ScienceType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "SoSEntry")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_SoSEntry {
    @XmlElement(required = true, name = "ScienceType")
    private RS_ScienceType scienceType;

    @XmlElement(required = true, name = "Books")
    private Integer books;

    @XmlElement(required = true, name = "BooksInProgress")
    private Integer booksInProgress;

    @XmlElement(required = true, name = "Effect")
    private Double effect;

    public RS_SoSEntry() {
    }

    RS_SoSEntry(final ScienceType scienceType) {
        this.scienceType = RS_ScienceType.fromScienceType(scienceType, false);
    }

    public RS_ScienceType getScienceType() {
        return scienceType;
    }

    public Integer getBooks() {
        return books;
    }

    public void setBooks(final Integer books) {
        this.books = books;
    }

    public Integer getBooksInProgress() {
        return booksInProgress;
    }

    public void setBooksInProgress(final Integer booksInProgress) {
        this.booksInProgress = booksInProgress;
    }

    public Double getEffect() {
        return effect;
    }

    public void setEffect(final Double effect) {
        this.effect = effect;
    }
}
