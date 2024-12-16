import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;

public class ThreadsSiege extends JFrame implements KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GUN_Y = HEIGHT - 50;
    private static final int ENEMY_SIZE = 40;
    private static final int BULLET_SIZE = 10;
    private static final int MAX_ENEMIES = 10;
    private static final int WIN_SCORE = 15;
    private static final int LOSE_SCORE = 30;

    private int gunX = WIDTH / 2;
    private int score = 0;
    private int missed = 0;
    private boolean gameOver = false;
    private boolean gameWon = false;
    private int cameraShakeOffset = 0;

    private List<Enemy> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private Semaphore bulletLimit = new Semaphore(3);
    private Lock screenLock = new ReentrantLock();
    private Condition enemiesGenerated = screenLock.newCondition();
    private boolean enemyGenerationStarted = false;

    private long gameStartTime;

    private GamePanel gamePanel;

    public ThreadsSiege() {
        setTitle("Threads Siege - Влучено: 0, Пропущено: 0");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addKeyListener(this);
        setResizable(true);

        gamePanel = new GamePanel();
        add(gamePanel);

        gameStartTime = System.currentTimeMillis();

        // Запуск потоку генерування ворогів
        new EnemyGenerator().start();
        setVisible(true);
    }

    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // переконатись, що панель правильно очищена
    
            screenLock.lock(); // блокування для забезпечення малювання спільних ресурсів
            try {
                int panelWidth = getWidth();
                int panelHeight = getHeight();

                ////////////////////
                // ФОН
                ////////////////////

                // ФОН – основа
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 30), 0, panelHeight, new Color(60, 60, 80));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, panelWidth, panelHeight);

                // ФОН – сітка
                g.setColor(new Color(255, 255, 255, 10));
                for (int i = 0; i < panelWidth; i += 40) {
                    g.drawLine(i, 0, i, panelHeight); // вертикальні
                }
                for (int j = 0; j < panelHeight; j += 40) {
                    g.drawLine(0, j, panelWidth, j); // горизонтальні
                }

                // ФОН – випадкові зірки (100)
                Random random = new Random();
                g.setColor(new Color(255, 255, 255, 80)); 
                for (int i = 0; i < 100; i++) {
                    int starX = random.nextInt(panelWidth);
                    int starY = random.nextInt(panelHeight);
                    int starSize = random.nextInt(3) + 1;
                    g.fillOval(starX, starY, starSize, starSize);
                }

                ////////////////////
                // КАМЕРИ. Ефекти
                ////////////////////
                int shakeX = cameraShakeOffset != 0 ? (int) (Math.random() * 10 - 5) : 0;
                int shakeY = cameraShakeOffset != 0 ? (int) (Math.random() * 10 - 5) : 0;

                // поступове зменшення ефекту
                if (cameraShakeOffset > 0) {
                    cameraShakeOffset--;
                }
    
                ////////////////////
                // КОСМІЧНИЙ КОРАБЕЛЬ
                ////////////////////

                // основне тіло
                g.setColor(new Color(180, 160, 150)); 
                g.fillRoundRect(gunX + shakeX, GUN_Y + shakeY, 50, 15, 10, 10);

                // зброя. основа
                g.setColor(new Color(120, 110, 100));
                g.fillRect(gunX + shakeX + 50, GUN_Y + shakeY + 4, 20, 6);

                // зброя. дуло
                g.setColor(new Color(200, 180, 170));
                g.fillOval(gunX + shakeX + 68, GUN_Y + shakeY + 2, 10, 10);

                // зброя. деталі
                g.setColor(new Color(100, 90, 80));
                g.fillRect(gunX + shakeX + 10, GUN_Y + shakeY + 5, 30, 3);
                g.setColor(new Color(100, 90, 80));
                g.fillOval(gunX + shakeX + 5, GUN_Y + shakeY + 2, 4, 4);

                ////////////////////
                // СУПРОТИВНИК
                ////////////////////
                
                for (Enemy enemy : enemies) {
                    // основа тіла
                    g.setColor(new Color(188, 156, 176));
                    g.fillOval(enemy.x + shakeX, enemy.y + shakeY, ENEMY_SIZE, ENEMY_SIZE);
                    g.setColor(new Color(150, 120, 140));
                    g.fillOval(enemy.x + 5 + shakeX, enemy.y + 5 + shakeY, ENEMY_SIZE - 10, ENEMY_SIZE - 10);
                    
                    // кільця на тілі
                    g.setColor(new Color(210, 180, 200));
                    g.drawOval(enemy.x + 15 + shakeX, enemy.y + 15 + shakeY, ENEMY_SIZE - 30, ENEMY_SIZE - 30);
                
                    // антени
                    g.setColor(new Color(100, 80, 100));
                    g.drawLine(enemy.x + 15 + shakeX, enemy.y + shakeY, enemy.x + 10 + shakeX, enemy.y - 10 + shakeY); // ліворуч
                    g.drawLine(enemy.x + 25 + shakeX, enemy.y + shakeY, enemy.x + 30 + shakeX, enemy.y - 10 + shakeY); // праворуч
                
                    // очі. основа
                    g.setColor(new Color(240, 201, 135));
                    g.fillOval(enemy.x + 10 + shakeX, enemy.y + 10 + shakeY, 20, 20);
                    g.setColor(new Color(255, 255, 255, 150));
                    g.fillOval(enemy.x + 12 + shakeX, enemy.y + 12 + shakeY, 10, 10);
                    g.fillOval(enemy.x + 22 + shakeX, enemy.y + 12 + shakeY, 10, 10);
                
                    // очі. середина
                    g.setColor(Color.BLACK); 
                    g.fillOval(enemy.x + 15 + shakeX, enemy.y + 15 + shakeY, 5, 5);
                    g.fillOval(enemy.x + 25 + shakeX, enemy.y + 15 + shakeY, 5, 5);
                
                    // деталі. рот
                    g.setColor(new Color(150, 80, 90));
                    g.drawLine(enemy.x + 17 + shakeX, enemy.y + 25 + shakeY, enemy.x + 28 + shakeX, enemy.y + 25 + shakeY);
                
                    // деталі. лапи
                    g.setColor(new Color(80, 60, 90));
                    g.drawLine(enemy.x + 10 + shakeX, enemy.y + ENEMY_SIZE + shakeY, enemy.x + 10 + shakeX, enemy.y + ENEMY_SIZE + 10 + shakeY);
                    g.drawLine(enemy.x + 30 + shakeX, enemy.y + ENEMY_SIZE + shakeY, enemy.x + 30 + shakeX, enemy.y + ENEMY_SIZE + 10 + shakeY);
                }

                ////////////////////
                // КУЛЯ
                ////////////////////

                g.setColor(Color.WHITE);
                for (Bullet bullet : bullets) {
                    // сяйво довкола кулі
                    g.setColor(new Color(255, 255, 255, 100)); 
                    g.fillOval(bullet.x + shakeX - 4, bullet.y + shakeY - 4, BULLET_SIZE + 8, BULLET_SIZE + 8); 

                    // основне тіло кулі
                    g.setColor(new Color(200, 220, 255));
                    g.fillOval(bullet.x + shakeX, bullet.y + shakeY, BULLET_SIZE, BULLET_SIZE);

                    // деталізовані контури
                    g.setColor(new Color(240, 250, 255));
                    g.fillOval(bullet.x + 2 + shakeX, bullet.y + 2 + shakeY, BULLET_SIZE - 4, BULLET_SIZE - 4);
                }
  
                ////////////////////
                // ІНТЕРФЕЙС. Рахунок
                ////////////////////

                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(15f));
                g.drawString("Влучено: " + score, 10, 20);
                g.drawString("Пропущено: " + missed, 10, 35);
    
                setTitle("Threads Siege - Влучено: " + score + ", Пропущено: " + missed);
            } finally {
                screenLock.unlock();
            }
        }
    }

    private class Enemy extends Thread {
        int x, y, speed;
        private boolean active = true; // відстеження чи ворог ще активний

        public Enemy(int x, int speed) {
            this.x = x; // початкова горизонтальна позиція
            this.y = 0; // початкова вертикальна позиція
            this.speed = speed; // швидкість ворога
        }

        @Override
        public void run() {
            try {
                while (!gameOver && active) { // продовжувати, поки гра не завершена й ворог активний
                    Thread.sleep(100);
                    screenLock.lock(); // узгодження оновлення спільних змінних
                    try {
                        y += speed; // переміщення ворога вниз
                        if (y > HEIGHT) { // перевірка досягенння нижньої частини
                            missed++;
                            cameraShake();
                            if (missed >= LOSE_SCORE) {
                                gameOver = true;
                                showGameOver();
                            }
                            active = false; // позначити ворога неактивним як вийде за екран
                            enemies.remove(this);
                        }
                    } finally {
                        screenLock.unlock(); // розблокування після оновлення позиції
                    }
                    gamePanel.repaint(); // перемалювання панель для оновлення позиції
                }
            } catch (InterruptedException e) { // оброблення непередбачених переривань
                Thread.currentThread().interrupt();
            }
        }

        public boolean isActive() {
            return active;
        }

        public void deactivate() {
            active = false;
        }
    }

    private class Bullet extends Thread {
        int x, y;
        private boolean active = true; // відстеження чи куля ще активна

        public Bullet(int x) {
            this.x = x; // початкова горизонтальна позиція
            this.y = GUN_Y; // початкова вертикальна позиція
        }

        @Override
        public void run() {
            try {
                while (y > 0 && active) { // продовжувати, поки куля на екрані та активна
                    Thread.sleep(50);
                    screenLock.lock(); // узгодження оновлення змінних
                    try { 
                        y -= 10; // переміщення кулі вгору
                        checkCollisionWithEnemies(); // перевірка на зіткнення з ворогом
                    } finally {
                        screenLock.unlock(); // розблокування після оновлення позиції
                    }
                    gamePanel.repaint(); // перемалювання панель для оновлення позиції
                }
            } catch (InterruptedException e) { // оброблення непередбачених переривань
                Thread.currentThread().interrupt();
            } finally {
                bulletLimit.release(); // звільнити semaphore, коли куля більше не активна
                bullets.remove(this);
            }
        }

        private void checkCollisionWithEnemies() {
            Iterator<Enemy> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();
                if (enemy.isActive() && x >= enemy.x && x <= enemy.x + ENEMY_SIZE &&
                    y >= enemy.y && y <= enemy.y + ENEMY_SIZE) { // якщо куля потрапила у ворога
                    enemy.deactivate();
                    active = false;
                    score++;
                    cameraShake();
                    enemyIterator.remove(); // безпечно вилучити ворога зі списку
                    checkWinningCondition();
                    break;
                }
            }
        }
    }

    private class EnemyGenerator extends Thread {
        private int delay = 1500; // відрегульована швидкість появи ворогів
        private Random rand = new Random();

        @Override
        public void run() {
            try {
                while (!gameOver && !gameWon) { // продовжити поки гра активна
                    Thread.sleep(delay);
                    screenLock.lock(); // узгодження оновлення спільних змінних
                    try {
                        if (!enemyGenerationStarted) { // чекати поки не буде сигналу
                            enemiesGenerated.await();
                        }
                        
                        int maxDynamicEnemies = getDynamicMaxEnemies(); // отримати поточну динамічну максимальну кількість ворогів
                        if (enemies.size() < maxDynamicEnemies) {
                            int x = rand.nextInt(WIDTH - ENEMY_SIZE); // випадкова горизонтальна позиція
                            int speed = rand.nextInt(5) + 1;
                            Enemy enemy = new Enemy(x, speed);
                            enemies.add(enemy);
                            enemy.start();
                        }

                        if (delay > 500) { // зменшити затримку появи ворогів з часом 
                            delay -= 100;
                        }
                    } finally {
                        screenLock.unlock(); // розблокувати після генерування
                    }
                    gamePanel.repaint(); // перемалювання панель для оновлення позиції
                }
            } catch (InterruptedException e) { // оброблення непередбачених переривань
                Thread.currentThread().interrupt();
            }
        }
    }

    private int getDynamicMaxEnemies() {
        // розрахунок відсотку MAX_ENEMIES на основі прогресу гравця
        double scoreFactor = (double) score / (WIN_SCORE * 2);
        double timeFactor = Math.min(1.0, (System.currentTimeMillis() - gameStartTime) / (double) (60 * 1000));

        // динамічна кількість поточних ворогів
        double progress = Math.min(1.0, scoreFactor + timeFactor);

        return (int) (MAX_ENEMIES * Math.max(0.1, progress));
    }

    // методи перевірки досягнення гравцем переможних умов
    private void checkWinningCondition() {
        if (score >= WIN_SCORE) {
            gameWon = true;
            showWinningScreen();
        }
    }

    private void cameraShake() {
        cameraShakeOffset = 10; // інтенсивність ефекту
    }

    private void showWinningScreen() {
        screenLock.lock();
        try {
            SwingUtilities.invokeLater(() -> {
                JPanel winningPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
    
                        // Фон
                        g.setColor(new Color(220, 255, 220));
                        g.fillRect(0, 0, getWidth(), getHeight());
    
                        // Текст
                        g.setFont(g.getFont().deriveFont(50f));
                        g.setColor(new Color(50, 150, 50)); 
                        String winMessage = "You Won!";
                        int textWidth = g.getFontMetrics().stringWidth(winMessage);
                        g.drawString(winMessage, (getWidth() - textWidth) / 2, getHeight() / 2 - 20);
    
                        // Рахунок
                        g.setFont(g.getFont().deriveFont(25f));
                        String scoreMessage = "Final Score: " + score;
                        int scoreWidth = g.getFontMetrics().stringWidth(scoreMessage);
                        g.drawString(scoreMessage, (getWidth() - scoreWidth) / 2, getHeight() / 2 + 40);
                    }
                };
    
                // показ панелі
                winningPanel.setSize(getSize());
                setContentPane(winningPanel);
                revalidate();
                repaint();
    
                // затримка 4 секунди, вихід
                new Timer(4000, e -> System.exit(0)).start();
            });
        } finally {
            screenLock.unlock();
        }
    }

    private void showGameOver() {
        screenLock.lock();
        try {
            SwingUtilities.invokeLater(() -> {
                JPanel gameOverPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
    
                        // Фон
                        g.setColor(new Color(255, 230, 230));
                        g.fillRect(0, 0, getWidth(), getHeight());
    
                        // Текст
                        g.setFont(g.getFont().deriveFont(50f));
                        g.setColor(new Color(180, 50, 50));
                        String gameOverMessage = "Game Over";
                        int textWidth = g.getFontMetrics().stringWidth(gameOverMessage);
                        g.drawString(gameOverMessage, (getWidth() - textWidth) / 2, getHeight() / 2 - 20);
    
                        // Рахунок
                        g.setFont(g.getFont().deriveFont(25f));
                        String scoreMessage = "Final Score: " + score;
                        int scoreWidth = g.getFontMetrics().stringWidth(scoreMessage);
                        g.drawString(scoreMessage, (getWidth() - scoreWidth) / 2, getHeight() / 2 + 40);
                    }
                };
    
                // показ панелі
                gameOverPanel.setSize(getSize());
                setContentPane(gameOverPanel);
                revalidate();
                repaint();
    
                // затримка 4 секунди, вихід
                new Timer(4000, e -> System.exit(0)).start();
            });
        } finally {
            screenLock.unlock();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver || gameWon) return; // ігнорувати натискання клавіш, якщо гра завершена

        screenLock.lock();
        try {
            if (e.getKeyCode() == KeyEvent.VK_LEFT && gunX > 0) {
                gunX -= 10; // гармату ліворуч
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && gunX < WIDTH - 50) {
                gunX += 10; // гармату праворуч
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (bulletLimit.tryAcquire()) { // спроба захопити семафор перед створенням
                    Bullet bullet = new Bullet(gunX + 20);
                    bullets.add(bullet);
                    bullet.start();
                }
            }

            if (!enemyGenerationStarted) {
                enemyGenerationStarted = true;
                screenLock.lock();
                try {
                    enemiesGenerated.signal(); // сигналізувати для початку генерування
                } finally {
                    screenLock.unlock();
                }
            }
        } finally {
            screenLock.unlock();
        }
        gamePanel.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ThreadsSiege());
    }
}