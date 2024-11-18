package br.unitins.tp1.notebooks.service;

import java.util.List;
import java.util.stream.Collectors;

import br.unitins.tp1.notebooks.dto.LoteRequestDTO;
import br.unitins.tp1.notebooks.dto.LoteResponseDTO;
import br.unitins.tp1.notebooks.modelo.Lote;
import br.unitins.tp1.notebooks.modelo.Notebook;
import br.unitins.tp1.notebooks.repository.LoteRepository;
import br.unitins.tp1.notebooks.repository.NotebookRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LoteServiceImpl implements LoteService {

    @Inject
    private LoteRepository loteRepository;

    @Inject
    private NotebookRepository notebookRepository;

    @Override
    public List<LoteResponseDTO> findAll() {
        return loteRepository.findAll()
                .stream()
                .map(LoteResponseDTO::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public LoteResponseDTO findById(Long id) {
        Lote lote = loteRepository.findById(id);
        if (lote == null) {
            throw new IllegalArgumentException("Lote não encontrado com o ID: " + id);
        }
        return LoteResponseDTO.valueOf(lote);
    }

    @Override
    @Transactional
    public LoteResponseDTO create(LoteRequestDTO dto) {
        Notebook notebook = notebookRepository.findById(dto.notebookId());
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook não encontrado com o ID: " + dto.notebookId());
        }

        Lote lote = new Lote(notebook, dto.quantidade());
        lote.setDataEntrada(dto.dataEntrada());
        loteRepository.persist(lote);

        return LoteResponseDTO.valueOf(lote);
    }

    @Override
    @Transactional
    public LoteResponseDTO update(Long id, LoteRequestDTO dto) {
        Lote lote = loteRepository.findById(id);
        if (lote == null) {
            throw new IllegalArgumentException("Lote não encontrado com o ID: " + id);
        }

        Notebook notebook = notebookRepository.findById(dto.notebookId());
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook não encontrado com o ID: " + dto.notebookId());
        }

        lote.setNotebook(notebook);
        lote.setQuantidade(dto.quantidade());
        lote.setDataEntrada(dto.dataEntrada());

        return LoteResponseDTO.valueOf(lote);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!loteRepository.deleteById(id)) {
            throw new IllegalArgumentException("Erro ao excluir o lote com ID: " + id);
        }
    }

    @Override
    public int verificarEstoque(Long notebookId) {
        // Verifica se o notebook existe
        Notebook notebook = notebookRepository.findById(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook não encontrado com o ID: " + notebookId);
        }

        // Calcula o estoque total
        return loteRepository.findByNotebookId(notebookId)
                .stream()
                .mapToInt(Lote::getQuantidade)
                .sum();
    }

    @Override
    @Transactional
    public void atualizarEstoque(Long notebookId, int quantidade) {
        List<Lote> lotes = loteRepository.findByNotebookId(notebookId);

        if (lotes.isEmpty()) {
            throw new IllegalArgumentException("Nenhum lote encontrado para o notebook com ID: " + notebookId);
        }

        int restante = quantidade;

        for (Lote lote : lotes) {
            int quantidadeLote = lote.getQuantidade();

            if (restante <= 0) {
                break; // Quantidade já foi totalmente reduzida.
            }

            if (quantidadeLote <= restante) {
                // Reduzir tudo do lote e continuar para o próximo.
                restante -= quantidadeLote;
                lote.setQuantidade(0);
            } else {
                // Reduzir parte do lote e finalizar.
                lote.setQuantidade(quantidadeLote - restante);
                restante = 0;
            }

            loteRepository.persist(lote); // Atualiza o lote no banco.
        }

        if (restante > 0) {
            throw new IllegalArgumentException("Estoque insuficiente para o notebook com ID: " + notebookId);
        }
    }
}