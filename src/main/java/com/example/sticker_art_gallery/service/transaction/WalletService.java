package com.example.sticker_art_gallery.service.transaction;

import com.example.sticker_art_gallery.exception.WalletNotFoundException;
import com.example.sticker_art_gallery.model.transaction.UserWalletEntity;
import com.example.sticker_art_gallery.model.transaction.UserWalletRepository;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с кошельками пользователей
 */
@Service
@Transactional
public class WalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    private final UserWalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletService(UserWalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    /**
     * Привязать кошелек к пользователю
     * Перед созданием нового кошелька деактивирует все старые активные кошельки пользователя
     */
    public UserWalletEntity linkWallet(Long userId, String walletAddress, String walletType) {
        LOGGER.info("Привязка кошелька: userId={}, address={}, type={}", userId, walletAddress, walletType);
        
        // Проверяем, существует ли уже такой кошелек
        Optional<UserWalletEntity> existing = walletRepository.findByUser_IdAndWalletAddress(userId, walletAddress);
        if (existing.isPresent()) {
            LOGGER.debug("Кошелек уже привязан, активируем его");
            UserWalletEntity wallet = existing.get();
            
            // Деактивируем все остальные активные кошельки пользователя
            deactivateAllActiveWalletsExcept(userId, wallet.getId());
            
            wallet.setIsActive(true);
            if (walletType != null) {
                wallet.setWalletType(walletType);
            }
            return walletRepository.save(wallet);
        }

        // Деактивируем все старые активные кошельки пользователя перед созданием нового
        deactivateAllActiveWallets(userId);

        // Создаем новый кошелек
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Пользователь с ID " + userId + " не найден");
        }

        UserWalletEntity wallet = new UserWalletEntity();
        wallet.setUser(userOpt.get());
        wallet.setWalletAddress(walletAddress);
        wallet.setWalletType(walletType);
        wallet.setIsActive(true);

        return walletRepository.save(wallet);
    }

    /**
     * Получить активный кошелёк пользователя
     * Возвращает самый свежий активный кошелёк (сортировка по created_at DESC)
     * 
     * @param userId ID пользователя
     * @return активный кошелёк
     * @throws WalletNotFoundException если активный кошелёк не найден
     */
    @Transactional(readOnly = true)
    public UserWalletEntity getActiveWallet(Long userId) {
        List<UserWalletEntity> activeWallets = walletRepository.findByUser_IdAndIsActiveTrue(userId);
        
        if (activeWallets.isEmpty()) {
            throw new WalletNotFoundException(userId);
        }
        
        // Сортировка по created_at DESC (самый свежий первым)
        List<UserWalletEntity> sortedWallets = activeWallets.stream()
            .sorted(Comparator.comparing(UserWalletEntity::getCreatedAt).reversed())
            .collect(Collectors.toList());
        
        return sortedWallets.get(0);
    }

    /**
     * Деактивировать все активные кошельки пользователя
     */
    private void deactivateAllActiveWallets(Long userId) {
        List<UserWalletEntity> activeWallets = walletRepository.findByUser_IdAndIsActiveTrue(userId);
        for (UserWalletEntity wallet : activeWallets) {
            wallet.setIsActive(false);
            walletRepository.save(wallet);
        }
        if (!activeWallets.isEmpty()) {
            LOGGER.debug("Деактивировано {} активных кошельков пользователя {}", activeWallets.size(), userId);
        }
    }

    /**
     * Деактивировать все активные кошельки пользователя, кроме указанного
     */
    private void deactivateAllActiveWalletsExcept(Long userId, Long exceptWalletId) {
        List<UserWalletEntity> activeWallets = walletRepository.findByUser_IdAndIsActiveTrue(userId);
        for (UserWalletEntity wallet : activeWallets) {
            if (!wallet.getId().equals(exceptWalletId)) {
                wallet.setIsActive(false);
                walletRepository.save(wallet);
            }
        }
    }

    /**
     * Получить активные кошельки пользователя
     */
    @Transactional(readOnly = true)
    public List<UserWalletEntity> getActiveWallets(Long userId) {
        return walletRepository.findByUser_IdAndIsActiveTrue(userId);
    }

    /**
     * Деактивировать кошелек
     */
    public void deactivateWallet(Long walletId) {
        LOGGER.info("Деактивация кошелька: walletId={}", walletId);
        Optional<UserWalletEntity> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isPresent()) {
            UserWalletEntity wallet = walletOpt.get();
            wallet.setIsActive(false);
            walletRepository.save(wallet);
        } else {
            throw new IllegalArgumentException("Кошелек с ID " + walletId + " не найден");
        }
    }
}

