package com.vetsync.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.MedicalRecord;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MedicalRecordRepositoryAdapterTest {

    private final SpringDataMedicalRecordRepository medicalRecords = mock(SpringDataMedicalRecordRepository.class);
    private final MedicalRecordRepositoryAdapter adapter = new MedicalRecordRepositoryAdapter(medicalRecords);

    @Test
    void findByIdReturnsRecordOrEmpty() {
        MedicalRecord record = mock(MedicalRecord.class);
        when(medicalRecords.findById(20L)).thenReturn(Optional.of(record));
        when(medicalRecords.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(20L)).containsSame(record);
        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void existsByIdAndOwnerIdUsesOwnerScopedQuery() {
        when(medicalRecords.existsByIdAndPet_Owner_Id(20L, 5L)).thenReturn(true);

        assertThat(adapter.existsByIdAndOwnerId(20L, 5L)).isTrue();
    }

    @Test
    void findByPetIdReturnsRecordsNewestFirstFromSpringData() {
        List<MedicalRecord> found = List.of(mock(MedicalRecord.class));
        when(medicalRecords.findAllByPet_IdOrderByVisitDateDescIdDesc(10L)).thenReturn(found);

        assertThat(adapter.findByPetId(10L)).isSameAs(found);
    }

    @Test
    void saveReturnsPersistedRecord() {
        MedicalRecord record = mock(MedicalRecord.class);
        when(medicalRecords.save(record)).thenReturn(record);

        assertThat(adapter.save(record)).isSameAs(record);
    }

    @Test
    void deleteDelegatesToSpringData() {
        MedicalRecord record = mock(MedicalRecord.class);

        adapter.delete(record);

        verify(medicalRecords).delete(record);
    }
}
