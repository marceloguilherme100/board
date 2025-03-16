package br.com.dio.ui;

import br.com.dio.dto.BoardColumnInfoDTO;
import br.com.dio.persistence.entity.BoardColumnEntity;
import br.com.dio.persistence.entity.BoardEntity;
import br.com.dio.persistence.entity.CardEntity;
import br.com.dio.service.*;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.Scanner;
import static br.com.dio.persistence.config.ConnectionConfig.getConnection;

@AllArgsConstructor
public class BoardMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");
    private final BoardEntity entity;

    public void execute() {
        try {
            System.out.printf("Bem-vindo ao board %s! Selecione a operação desejada:\n", entity.getId());
            int option;
            do {
                printMenu();
                option = getValidIntegerInput();
                handleOption(option);
            } while (option != 9);
        } catch (SQLException ex) {
            System.err.println("Erro ao executar operação: " + ex.getMessage());
        }
    }

    private void printMenu() {
        System.out.println("1 - Criar um card");
        System.out.println("2 - Mover um card");
        System.out.println("3 - Bloquear um card");
        System.out.println("4 - Desbloquear um card");
        System.out.println("5 - Cancelar um card");
        System.out.println("6 - Ver board");
        System.out.println("7 - Ver coluna com cards");
        System.out.println("8 - Ver card");
        System.out.println("9 - Voltar para o menu anterior");
        System.out.println("10 - Sair");
        System.out.print("Escolha uma opção: ");
    }

    private int getValidIntegerInput() {
        try {
            return scanner.nextInt();
        } catch (InputMismatchException e) {
            scanner.next();
            System.out.println("Opção inválida! Informe um número válido.");
            return -1;
        }
    }

    private void handleOption(int option) throws SQLException {
        switch (option) {
            case 1 -> createCard();
            case 2 -> moveCardToNextColumn();
            case 3 -> blockCard();
            case 4 -> unblockCard();
            case 5 -> cancelCard();
            case 6 -> showBoard();
            case 7 -> showColumn();
            case 8 -> showCard();
            case 9 -> System.out.println("Voltando para o menu anterior...");
            case 10 -> System.exit(0);
            default -> System.out.println("Opção inválida! Escolha um número do menu.");
        }
    }

    private void createCard() throws SQLException {
        var card = new CardEntity();
        System.out.print("Informe o título do card: ");
        card.setTitle(scanner.next());
        System.out.print("Informe a descrição do card: ");
        card.setDescription(scanner.next());
        card.setBoardColumn(entity.getInitialColumn());
        try (var connection = getConnection()) {
            new CardService(connection).create(card);
            System.out.println("Card criado com sucesso!");
        }
    }

    private void moveCardToNextColumn() throws SQLException {
        System.out.print("Informe o ID do card que deseja mover: ");
        long cardId = scanner.nextLong();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try (var connection = getConnection()) {
            new CardService(connection).moveToNextColumn(cardId, boardColumnsInfo);
            System.out.println("Card movido com sucesso!");
        } catch (RuntimeException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private void blockCard() throws SQLException {
        System.out.print("Informe o ID do card a ser bloqueado: ");
        long cardId = scanner.nextLong();
        System.out.print("Informe o motivo do bloqueio: ");
        String reason = scanner.next();
        try (var connection = getConnection()) {
            new CardService(connection).block(cardId, reason);
            System.out.println("Card bloqueado com sucesso!");
        }
    }

    private void unblockCard() throws SQLException {
        System.out.print("Informe o ID do card a ser desbloqueado: ");
        long cardId = scanner.nextLong();
        System.out.print("Informe o motivo do desbloqueio: ");
        String reason = scanner.next();
        try (var connection = getConnection()) {
            new CardService(connection).unblock(cardId, reason);
            System.out.println("Card desbloqueado com sucesso!");
        }
    }

    private void cancelCard() throws SQLException {
        System.out.print("Informe o ID do card para cancelar: ");
        long cardId = scanner.nextLong();
        var cancelColumn = entity.getCancelColumn();
        try (var connection = getConnection()) {
            new CardService(connection).cancel(cardId, cancelColumn.getId());
            System.out.println("Card cancelado com sucesso!");
        }
    }

    private void showBoard() throws SQLException {
        try (var connection = getConnection()) {
            new BoardQueryService(connection).showBoardDetails(entity.getId())
                .ifPresent(b -> {
                    System.out.printf("Board [%s, %s]\n", b.id(), b.name());
                    b.columns().forEach(c ->
                        System.out.printf("Coluna [%s] tipo: [%s] tem %s cards\n", c.name(), c.kind(), c.cardsAmount()));
                });
        }
    }

    private void showColumn() throws SQLException {
        entity.getBoardColumns().forEach(c ->
            System.out.printf("%s - %s [%s]\n", c.getId(), c.getName(), c.getKind()));
        System.out.print("Escolha o ID da coluna: ");
        long selectedColumnId = scanner.nextLong();
        try (var connection = getConnection()) {
            new BoardColumnQueryService(connection).findById(selectedColumnId)
                .ifPresent(col -> {
                    System.out.printf("Coluna %s tipo %s\n", col.getName(), col.getKind());
                    col.getCards().forEach(ca -> System.out.printf("Card %s - %s\nDescrição: %s\n",
                            ca.getId(), ca.getTitle(), ca.getDescription()));
                });
        }
    }

    private void showCard() throws SQLException {
        System.out.print("Informe o ID do card: ");
        long cardId = scanner.nextLong();
        try (var connection = getConnection()) {
            new CardQueryService(connection).findById(cardId)
                .ifPresentOrElse(
                    c -> {
                        System.out.printf("Card %s - %s\nDescrição: %s\n", c.id(), c.title(), c.description());
                    },
                    () -> System.out.printf("Não existe um card com o ID %s\n", cardId)
                );
        }
    }
}
