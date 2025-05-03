import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class SimuladorMemoriaAlocada extends JFrame {

    private final JComboBox<String> caixaDeEstrategias;
    private final DefaultListModel<String> modeloListaProcessos;
    private final List<BlocoMemoria> blocosDeMemoria;
    private final List<Processo> processos;

    private final JPanel painelMemoria;
    private final JButton botaoAlocar, botaoReiniciar, botaoSimularES;
    private final JTextField campoNome, campoTamanho;
    private JLabel statusMemoria;
    private int proximoNextFit = 0;

    public SimuladorMemoriaAlocada() {
        super("Simulador de Alocação de Memória");
        setLayout(new BorderLayout());

        caixaDeEstrategias = new JComboBox<>(new String[]{
                "First Fit", "Best Fit", "Worst Fit", "Next Fit"
        });

        modeloListaProcessos = new DefaultListModel<>();
        processos = new ArrayList<>();
        blocosDeMemoria = new ArrayList<>(Arrays.asList(
                new BlocoMemoria(0, 100),
                new BlocoMemoria(1, 150),
                new BlocoMemoria(2, 200),
                new BlocoMemoria(3, 250),
                new BlocoMemoria(4, 300),
                new BlocoMemoria(5, 350)
        ));

        JPanel painelEntrada = new JPanel(new GridLayout(2, 1));
        JPanel painelProcesso = new JPanel();

        painelProcesso.add(new JLabel("Nome:"));
        campoNome = new JTextField(5);
        painelProcesso.add(campoNome);

        painelProcesso.add(new JLabel("Tamanho:"));
        campoTamanho = new JTextField(5);
        painelProcesso.add(campoTamanho);

        botaoAlocar = new JButton("Alocar");
        painelProcesso.add(botaoAlocar);
        botaoReiniciar = new JButton("Reiniciar");
        painelProcesso.add(botaoReiniciar);
        botaoSimularES = new JButton("Simular E/S bloqueante");
        painelProcesso.add(botaoSimularES);
        painelEntrada.add(painelProcesso);

        JPanel painelEstrategia = new JPanel();
        painelEstrategia.add(new JLabel("Estratégia:"));
        painelEstrategia.add(caixaDeEstrategias);
        painelEntrada.add(painelEstrategia);
        add(painelEntrada, BorderLayout.NORTH);

        painelMemoria = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                desenharBlocos(g);
            }
        };

        painelMemoria.setPreferredSize(new Dimension(600, 400));
        add(painelMemoria, BorderLayout.CENTER);

        JList<String> listaProcessos = new JList<>(modeloListaProcessos);
        add(new JScrollPane(listaProcessos), BorderLayout.EAST);

        statusMemoria = new JLabel("Memória: Total: 0KB | Ocupado: 0KB | Livre: 0KB");
        add(statusMemoria, BorderLayout.SOUTH);

        botaoAlocar.addActionListener(e -> {
            String nome = campoNome.getText().trim();
            int tamanho;
            try {
                tamanho = Integer.parseInt(campoTamanho.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Tamanho inválido");
                return;
            }

            String estrategia = (String) caixaDeEstrategias.getSelectedItem();
            Processo processo = new Processo(nome, tamanho);
            processos.add(processo);

            boolean sucesso = switch (estrategia) {
                case "First Fit" -> alocarFirstFit(processo);
                case "Best Fit" -> alocarBestFit(processo);
                case "Worst Fit" -> alocarWorstFit(processo);
                case "Next Fit" -> alocarNextFit(processo);
                default -> false;
            };

            if (sucesso) {
                modeloListaProcessos.addElement(processo.toString());
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível alocar o processo.");
            }

            atualizarStatusMemoria();
        });

        botaoReiniciar.addActionListener(e -> {
            processos.clear();
            modeloListaProcessos.clear();
            blocosDeMemoria.forEach(BlocoMemoria::limpar);
            proximoNextFit = 0;
            atualizarStatusMemoria();
            repaint();
        });

        botaoSimularES.addActionListener(e -> {
            if (processos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum processo em execução!");
                return;
            }
            new Thread(() -> {
                Processo processo = processos.get(new Random().nextInt(processos.size()));
                processo.bloqueado = true;
                repaint();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
                processo.bloqueado = false;
                repaint();
            }).start();
        });

        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean alocarFirstFit(Processo p) {
        for (BlocoMemoria bloco : blocosDeMemoria) {
            if (bloco.estaVazio() && bloco.tamanho >= p.tamanho) {
                bloco.alocar(p);
                return true;
            }
        }
        return false;
    }

    private boolean alocarBestFit(Processo p) {
        BlocoMemoria melhor = null;
        for (BlocoMemoria bloco : blocosDeMemoria) {
            if (bloco.estaVazio() && bloco.tamanho >= p.tamanho) {
                if (melhor == null || bloco.tamanho < melhor.tamanho) {
                    melhor = bloco;
                }
            }
        }
        if (melhor != null) {
            melhor.alocar(p);
            return true;
        }
        return false;
    }

    private boolean alocarWorstFit(Processo p) {
        BlocoMemoria pior = null;
        for (BlocoMemoria bloco : blocosDeMemoria) {
            if (bloco.estaVazio() && bloco.tamanho >= p.tamanho) {
                if (pior == null || bloco.tamanho > pior.tamanho) {
                    pior = bloco;
                }
            }
        }
        if (pior != null) {
            pior.alocar(p);
            return true;
        }
        return false;
    }

    private boolean alocarNextFit(Processo p) {
        int n = blocosDeMemoria.size();
        for (int i = 0; i < n; i++) {
            int indice = (proximoNextFit + i) % n;
            BlocoMemoria bloco = blocosDeMemoria.get(indice);
            if (bloco.estaVazio() && bloco.tamanho >= p.tamanho) {
                bloco.alocar(p);
                proximoNextFit = (indice + 1) % n;
                return true;
            }
        }
        return false;
    }

    private void desenharBlocos(Graphics g) {
        int y = 20;
        for (BlocoMemoria bloco : blocosDeMemoria) {
            g.setColor(bloco.processo == null ? Color.LIGHT_GRAY :
                    (bloco.processo.bloqueado ? Color.ORANGE : Color.GREEN));
            g.fillRect(50, y, 200, 40);
            g.setColor(Color.BLACK);
            g.drawRect(50, y, 200, 40);
            g.drawString("Bloco " + bloco.id + ": " + bloco.tamanho + "KB", 60, y + 15);

            if (bloco.processo != null) {
                g.drawString(bloco.processo.nome + " (" + bloco.processo.tamanho + "KB)", 60, y + 35);
            }
            y += 60;
        }
    }

    private void atualizarStatusMemoria() {
        int total = 0;
        int usado = 0;

        for (BlocoMemoria bloco : blocosDeMemoria) {
            total += bloco.tamanho;
            if (bloco.processo != null) {
                usado += bloco.tamanho;
            }
        }
        int livre = total - usado;
        statusMemoria.setText("Memória: Total: " + total + "KB | Ocupado: " + usado + "KB | Livre: " + livre + "KB");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimuladorMemoriaAlocada::new);
    }

    static class BlocoMemoria {
        int id, tamanho;
        Processo processo;

        BlocoMemoria(int id, int tamanho) {
            this.id = id;
            this.tamanho = tamanho;
        }

        boolean estaVazio() {
            return processo == null;
        }

        void alocar(Processo p) {
            this.processo = p;
        }

        void limpar() {
            this.processo = null;
        }
    }

    static class Processo {
        String nome;
        int tamanho;
        boolean bloqueado = false;

        Processo(String nome, int tamanho) {
            this.nome = nome;
            this.tamanho = tamanho;
        }

        public String toString() {
            return nome + " (" + tamanho + "KB)" + (bloqueado ? " [BLOQUEADO]" : "");
        }
    }
}
