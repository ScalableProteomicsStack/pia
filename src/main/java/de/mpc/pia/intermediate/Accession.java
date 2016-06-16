package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


/**
 * Accession for the intermediate structure.
 *
 * @author julian
 */
public class Accession implements Serializable {

    private static final long serialVersionUID = -363630218908728494L;

    /** ID of accession */
    private Long id;

    /** The accession string */
    private String accessionStr;

    /** IDs of the files, where this Accession is found in */
    private Set<Long> files;

    /** Further description for the accession, mapped via the file ID */
    private Map<Long, String> descriptions;

    /** the sequence of the protein, as reported in a database */
    private String dbSequence;

    /** the IDs of the searchDatabases this accession is found in */
    private Set<String> searchDatabaseRefs;

    /** Pointer to group of this accession */
    private Group pGroup;



    /**
     * Basic constructor for the accession, sets the group to null.
     *
     * @param id
     * @param accession
     * @param description
     */
    public Accession(Long id, String accession, String dbSequence) {
        this.id = id;
        this.accessionStr = accession;
        this.files = new HashSet<Long>();
        this.descriptions = new HashMap<Long, String>();
        this.dbSequence = dbSequence;
        this.searchDatabaseRefs = new HashSet<String>();
        this.pGroup = null;
    }

    /**
     * Basic constructor for the accession.
     */
    public Accession(long id, String accession, Set<Long> files,
            Map<Long, String> descriptions, String dbSequence,
            Set<String> searchDatabaseRefs, Group group) {
        this(id, accession, dbSequence);
        if (files != null) {
            this.files.addAll(files);
        }
        if (descriptions != null) {
            this.descriptions.putAll(descriptions);
        }
        if (searchDatabaseRefs != null) {
            this.searchDatabaseRefs.addAll(searchDatabaseRefs);
        }
        this.pGroup = group;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Accession)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        Accession objAccession = (Accession)obj;
        return new EqualsBuilder().
                append(id, objAccession.id).
                append(accessionStr, objAccession.accessionStr).
                append(descriptions, objAccession.descriptions).
                append(dbSequence, objAccession.dbSequence).
                append(searchDatabaseRefs, objAccession.searchDatabaseRefs).
                append(pGroup, objAccession.pGroup).
                isEquals();
    }


    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder(23, 31).
                append(id).
                append(accessionStr).
                append(descriptions).
                append(dbSequence).
                append(searchDatabaseRefs);

        if (pGroup != null) {
            hcb.append(pGroup.getID());
        }

        return hcb.toHashCode();
    }


    /**
     * Getter for the ID.
     * @return
     */
    public Long getID() {
        return id;
    }


    /**
     * Getter for the accession.
     * @return
     */
    public String getAccession() {
        return accessionStr;
    }


    /**
     * Adds the given fileID to the IDs of files.
     * @return
     */
    public void addFile(Long fileID) {
        files.add(fileID);
    }


    /**
     * Returns the set of file IDs, in which the accession occurs.
     * @return
     */
    public Set<Long> getFiles() {
        return files;
    }


    /**
     * Adds the given description into the descriptions map with fileID as key.
     * @return
     */
    public void addDescription(long fileID, String description) {
        if ((description != null) && !description.isEmpty()) {
            descriptions.put(fileID, description);
        }
    }


    /**
     * Getter for the description, which belongs to the file  with the given ID.
     * @return
     */
    public String getDescription(Long fileID) {
        String desc = null;

        if (fileID > 0) {
            desc = descriptions.get(fileID);
        } else {
            Set<String> differentDescriptions = new HashSet<String>();
            for (Map.Entry<Long, String> descIt : descriptions.entrySet()) {
                if ((descIt.getValue() != null) &&
                        !descIt.getValue().trim().isEmpty()) {
                    differentDescriptions.add(descIt.getValue().trim());
                }
            }

            if (!differentDescriptions.isEmpty()) {
                StringBuilder descSB = new StringBuilder();
                for (String description : differentDescriptions) {
                    descSB.append(description);
                    descSB.append(";");
                }

                desc = descSB.substring(0, descSB.length()-1);
            }
        }

        if ((desc == null) || desc.trim().isEmpty()) {
            desc = "no further description for this accession";
        }

        return desc;
    }


    /**
     * Getter for the descriptions
     * @return
     */
    public Map<Long, String> getDescriptions() {
        return descriptions;
    }


    /**
     * Tests, if the accession was found in the file with the given ID.
     * @return
     */
    public boolean foundInFile(Long fileID) {
        return files.contains(fileID);
    }


    /**
     * Getter for the dbSequence.
     * @return
     */
    public String getDbSequence() {
        return dbSequence;
    }


    /**
     * Setter for the dbSequence. Necessary, if a sequence is found in only some
     * input files.
     *
     * @return
     */
    public void setDbSequence(String sequence) {
        this.dbSequence = sequence;
    }


    /**
     * Adds the given dbRef to the searchDatabaseRefs.
     * @param dbRef
     */
    public void addSearchDatabaseRef(String dbRef) {
        searchDatabaseRefs.add(dbRef);
    }


    /**
     * Adds the given dbRefs to the searchDatabaseRefs.
     * @param dbRef
     */
    public void addSearchDatabaseRefs(Set<String> dbRefs) {
        searchDatabaseRefs.addAll(dbRefs);
    }


    /**
     * Getter for the searchDatabaseRefs.
     * @return
     */
    public Set<String> getSearchDatabaseRefs() {
        return searchDatabaseRefs;
    }


    /**
     * Getter for the Group.
     * @return
     */
    public Group getGroup() {
        return pGroup;
    }


    /**
     * Setter for the group.
     * @param group
     */
    public void setGroup(Group group) {
        pGroup = group;
    }
}
